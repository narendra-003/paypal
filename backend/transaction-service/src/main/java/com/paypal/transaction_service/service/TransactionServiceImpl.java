package com.paypal.transaction_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypal.transaction_service.entity.Transaction;
import com.paypal.transaction_service.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    public TransactionServiceImpl(TransactionRepository transactionRepository, ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Transaction createTransaction(Transaction transactionRequest) {
        System.out.println("Entered createTransaction()");

        Long senderId = transactionRequest.getSenderId();
        Long receiverId = transactionRequest.getReceiverId();
        Double amount = transactionRequest.getAmount();

        Transaction transaction = new Transaction();
        transaction.setSenderId(senderId);
        transaction.setReceiverId(receiverId);
        transaction.setAmount(amount);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setStatus("SUCCESS");

        System.out.println("Incoming Transaction object: " + transaction);
        Transaction savedTransaction = transactionRepository.save(transaction);
        System.out.println("Saved Transaction from DB: " + savedTransaction);

        return savedTransaction;
    }

    @Override
    public List<Transaction> getAllTransactions() {
        List<Transaction> transactions = transactionRepository.findAll();
        return transactions;
    }
}
