package com.paypal.transaction_service.controller;

import com.paypal.transaction_service.entity.Transaction;
import com.paypal.transaction_service.service.TransactionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/create")
    public ResponseEntity<Transaction> creteTransaction(@Valid @RequestBody Transaction transaction) {
        logger.info("Received request to create transaction for sender: {}, receiver: {}",
                transaction.getSenderId(), transaction.getReceiverId());
        logger.debug("Transaction details: {}", transaction);
        Transaction createdTransaction = transactionService.createTransaction(transaction);
        logger.info("Successfully created transaction with ID: {}", createdTransaction.getId());
        return ResponseEntity.status(HttpStatus.OK).body(createdTransaction);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        logger.info("Received request to fetch all transactions");
        List<Transaction> allTransactions = transactionService.getAllTransactions();
        logger.info("Retrieved {} transactions", allTransactions.size());
        return ResponseEntity.status(HttpStatus.OK).body(allTransactions);
    }
}
