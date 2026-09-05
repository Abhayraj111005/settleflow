package com.settleflow.controller;

import com.settleflow.entity.Transaction;
import com.settleflow.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<Transaction> create(
            @RequestBody Transaction transaction) {

        Transaction savedTransaction =
                transactionService.create(transaction);

        return ResponseEntity.ok(savedTransaction);
    }
}