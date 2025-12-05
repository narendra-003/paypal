package com.paypal.transaction_service.controller;

import com.paypal.transaction_service.entity.Transaction;
import com.paypal.transaction_service.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/create")
    public ResponseEntity<Transaction> creteTransaction(@Valid @RequestBody Transaction transaction) {
        Transaction createdTransaction = transactionService.createTransaction(transaction);
        return ResponseEntity.status(HttpStatus.OK).body(createdTransaction);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        List<Transaction> allTransactions = transactionService.getAllTransactions();
        return ResponseEntity.status(HttpStatus.OK).body(allTransactions);
    }
}
