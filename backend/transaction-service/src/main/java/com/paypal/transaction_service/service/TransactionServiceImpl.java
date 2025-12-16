package com.paypal.transaction_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypal.transaction_service.entity.Transaction;
import com.paypal.transaction_service.exception.NotFoundException;
import com.paypal.transaction_service.kafka.KafkaEventProducer;
import com.paypal.transaction_service.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionServiceImpl.class);

    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;
    private final KafkaEventProducer kafkaEventProducer;
    private final RestTemplate restTemplate;

    public TransactionServiceImpl(TransactionRepository transactionRepository, ObjectMapper objectMapper, KafkaEventProducer kafkaEventProducer, RestTemplate restTemplate) {
        this.transactionRepository = transactionRepository;
        this.objectMapper = objectMapper;
        this.kafkaEventProducer = kafkaEventProducer;
        this.restTemplate = restTemplate;
    }

    @Override
    public Transaction createTransaction(Transaction transactionRequest) {
        logger.info("Creating transaction - Sender: {}, Receiver: {}, Amount: {}",
                transactionRequest.getSenderId(), transactionRequest.getReceiverId(), transactionRequest.getAmount());

        Long senderId = transactionRequest.getSenderId();
        Long receiverId = transactionRequest.getReceiverId();
        Double amount = transactionRequest.getAmount();

        // Step 0: Mark transaction as PENDING
        Transaction transaction = new Transaction();
        transaction.setSenderId(senderId);
        transaction.setReceiverId(receiverId);
        transaction.setAmount(amount);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setStatus("PENDING");
        Transaction savedTransaction = transactionRepository.save(transaction);
        logger.info("Transaction saved with PENDING status: ID={}, Sender={}, Receiver={}, Amount={}",
                savedTransaction.getId(), senderId, receiverId, amount);

        String walletServiceUrl = "http://localhost:8084/api/wallets"; // wallet service base URL
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Content-Type", "application/json");

        String holdReference = null;
        boolean captured = false; // whether capture (actual debit) completed

        try {
//           Step 1: Place hold on sender wallet
            holdReference = placeHold(savedTransaction);

            // NEW: check receiver wallet exists BEFORE capture
            try {
                ResponseEntity<String> receiverCheck = restTemplate.getForEntity(walletServiceUrl + "/" + receiverId, String.class);
                if (!receiverCheck.getStatusCode().is2xxSuccessful()) {
                    // release hold and fail the transaction
                    tryReleaseHold(walletServiceUrl, holdReference, httpHeaders);
                    logger.warn("Receiver wallet missing - Hold released: {} for transaction: {}", holdReference, savedTransaction.getId());
                    savedTransaction.setStatus("FAILED");
                    savedTransaction = transactionRepository.save(savedTransaction);
                    logger.error("Transaction FAILED - Receiver wallet missing: ID={}", savedTransaction.getId());
                    return savedTransaction;
                }
            } catch (HttpClientErrorException ex) {
                // receiver not found or other 4xx
                logger.error("Receiver wallet check failed for wallet ID {}: {}", receiverId, ex.getResponseBodyAsString());
                tryReleaseHold(walletServiceUrl, holdReference, httpHeaders);
                savedTransaction.setStatus("FAILED");
                savedTransaction = transactionRepository.save(savedTransaction);
                logger.error("Transaction FAILED - Receiver check error: ID={}", savedTransaction.getId());
                return savedTransaction;
            }

            // Step 2: Capture hold → debit sender wallet
            String captureJson = String.format("{\"holdReference\": \"%s\"}", holdReference);
            HttpEntity<String> captureEntity = new HttpEntity<>(captureJson, httpHeaders);
            ResponseEntity<String> captureResponse = restTemplate.postForEntity(walletServiceUrl + "/capture", captureEntity, String.class);

            if (!captureResponse.getStatusCode().is2xxSuccessful()) {
                // If capture failed, release hold and fail
                logger.error("Capture failed for hold {}: status={}, body={}",
                        holdReference, captureResponse.getStatusCode(), captureResponse.getBody());
                tryReleaseHold(walletServiceUrl, holdReference, httpHeaders);
                savedTransaction.setStatus("FAILED");
                savedTransaction = transactionRepository.save(savedTransaction);
                logger.error("Transaction FAILED - Capture failed: ID={}", savedTransaction.getId());
                return savedTransaction;
            }
            captured = true;
            logger.info("Hold captured successfully - Sender debited: Hold={}, Transaction={}", holdReference, savedTransaction.getId());

            // Step 3: Credit receiver wallet
            String creditJson = String.format("{\"userId\": %d, \"currency\": \"INR\", \"amount\": %.2f}", receiverId, amount);
            HttpEntity<String> creditEntity = new HttpEntity<>(creditJson, httpHeaders);
            try {
                ResponseEntity<String> creditResponse = restTemplate.postForEntity(walletServiceUrl + "/credit",
                        creditEntity, String.class);
                if(!creditResponse.getStatusCode().is2xxSuccessful()) {
                    throw new RuntimeException("Failed to credit receiver: status= " + creditResponse.getStatusCode());
                }
                logger.info("Receiver credited successfully: Receiver={}, Amount={}, Transaction={}",
                        receiverId, amount, savedTransaction.getId());
            } catch (HttpClientErrorException creditEx) {
                // Credit failed AFTER capture — perform compensating refund to sender
                logger.error("Credit failed for receiver {}: {}", receiverId, creditEx.getResponseBodyAsString());

                // Attempt to refund sender
                try {
                    String refundJson = String.format("{\"userId\": %d, \"currency\": \"INR\", \"amount\": %.2f}", senderId, amount);
                    HttpEntity<String> refundEntity = new HttpEntity<>(refundJson, httpHeaders);
                    ResponseEntity<String> refundResponse = restTemplate.postForEntity(
                            walletServiceUrl + "/credit", refundEntity, String.class);

                    if(refundResponse.getStatusCode().is2xxSuccessful()) {
                        logger.info("Compensating refund to sender succeeded: Sender={}, Amount={}, Transaction={}",
                                senderId, amount, savedTransaction.getId());
                    } else {
                        logger.error("Compensating refund returned non-2xx status: {}", refundResponse.getStatusCode());
                    }
                } catch (Exception ex) {
                    logger.error("Compensating refund to sender failed: {}", ex.getMessage(), ex);
                }

                savedTransaction.setStatus("FAILED");
                savedTransaction = transactionRepository.save(savedTransaction);
                logger.error("Transaction FAILED - Credit failed & refunded sender: ID={}", savedTransaction.getId());
                return savedTransaction;
            }

            // Step 4: Mark transaction as SUCCESS
            savedTransaction.setStatus("SUCCESS");
            savedTransaction = transactionRepository.save(savedTransaction);
            logger.info("Transaction SUCCESS: ID={}, Sender={}, Receiver={}, Amount={}",
                    savedTransaction.getId(), senderId, receiverId, amount);

        } catch (HttpClientErrorException ex) {
            logger.error("Wallet service error: {}", ex.getResponseBodyAsString(), ex);
            if (holdReference != null && !captured) {
                tryReleaseHold(walletServiceUrl, holdReference, httpHeaders);
            }

            savedTransaction.setStatus("FAILED");
            savedTransaction = transactionRepository.save(savedTransaction);
            logger.error("Transaction FAILED - 4xx error: ID={}", savedTransaction.getId());
            return savedTransaction;
        } catch (Exception ex) {
            logger.error("Transaction failed with exception: {}", ex.getMessage(), ex);
            if (holdReference != null && !captured) {
                tryReleaseHold(walletServiceUrl, holdReference, httpHeaders);
            }

            savedTransaction.setStatus("FAILED");
            savedTransaction = transactionRepository.save(savedTransaction);
            logger.error("Transaction FAILED - Exception occurred: ID={}", savedTransaction.getId());
            return savedTransaction;
        }

        // Step 6: Send Kafka Event
        try {
            String key = String.valueOf(savedTransaction.getId());
            kafkaEventProducer.sendTransactionEvent(key, savedTransaction); // actual txn object
            logger.info("Kafka event sent successfully for transaction: ID={}", savedTransaction.getId());
        } catch (Exception ex) {
            logger.error("Failed to send Kafka event for transaction {}: {}", savedTransaction.getId(), ex.getMessage(), ex);
        }

        return savedTransaction;
    }


    // Helper : release via path-style endpoint
    private void tryReleaseHold(String walletServiceUrl, String holdReference, HttpHeaders httpHeaders) {
        if(holdReference == null) return;

        try {
            // path-style release (matches WalletController: POST /release
            String releaseUrl = walletServiceUrl + "/release";
            String releaseJson = String.format("{\"holdReference\": \"%s\"}", holdReference);
            HttpEntity<String> releaseEntity = new HttpEntity<>(releaseJson, httpHeaders);

            logger.info("Attempting to release hold via: {} with reference: {}", releaseUrl, holdReference);
            ResponseEntity<String> releaseResponse = restTemplate.postForEntity(releaseUrl, releaseEntity, String.class);
            logger.info("Hold release response: status={}, body={}", releaseResponse.getStatusCode(), releaseResponse.getBody());
        } catch (Exception ex) {
            // log and move on (we don't want the whole transaction to crash on release failure)
            logger.error("Failed to release hold {}: {}", holdReference, ex.getMessage(), ex);
        }
    }

    @Override
    public List<Transaction> getAllTransactions() {
        List<Transaction> transactions = transactionRepository.findAll();
        return transactions;
    }

    @Override
    public List<Transaction> getTransactionByUser(Long userId) {
        List<Transaction> transactions = transactionRepository.findBySenderIdOrReceiverId(userId, userId);
        return transactions;
    }

    @Override
    public Transaction getTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transaction not found with ID: " + id));
        return transaction;
    }

    // Helper methods
    private ResponseEntity<String> post(String path, String body) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> httpEntity = new HttpEntity<>(body, httpHeaders);
        return restTemplate.postForEntity(walletUrl() + path, httpEntity, String.class);
    }

    private String walletUrl() {
        return "http://localhost:8084/api/wallets";
    }

    private String json(String fmt, Object... args) {
        return String.format(fmt, args);
    }

    private String placeHold(Transaction transaction) throws Exception{
        String holdJson = json("{\"userId\": %d, \"currency\": \"INR\", \"amount\": %.2f}",
                transaction.getSenderId(), transaction.getAmount());
        ResponseEntity<String> holdResponse = post("/hold", holdJson);

        if(!holdResponse.getStatusCode().is2xxSuccessful() || holdResponse.getBody() == null) {
            throw new RuntimeException("Failed to place hold: status= " + holdResponse.getStatusCode());
        }

        //Extract hold reference from response safely
        JsonNode holdNode = objectMapper.readTree(holdResponse.getBody());
        if(holdNode.get("holdReference") == null) {
            throw new RuntimeException("Hold response missing holdReference: " + holdResponse.getBody());
        }

        String holdReference = holdNode.get("holdReference").asText();
        logger.info("Hold placed successfully: Hold={}, Transaction={}, Amount={}",
                holdReference, transaction.getId(), transaction.getAmount());
        return holdReference;
    }

}
