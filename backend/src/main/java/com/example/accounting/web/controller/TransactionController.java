package com.example.accounting.web.controller;

import com.example.accounting.domain.Transaction;
import com.example.accounting.repository.TransactionRepository;
import com.example.accounting.service.TransactionService;
import com.example.accounting.web.dto.TransactionCreateRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;

    public TransactionController(TransactionRepository transactionRepository,
                                 TransactionService transactionService) {
        this.transactionRepository = transactionRepository;
        this.transactionService = transactionService;
    }

    @GetMapping
    public List<Transaction> list() {
        return transactionRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<Transaction> create(@Valid @RequestBody TransactionCreateRequest request) {
        Transaction tx = transactionService.create(request);
        return ResponseEntity.ok(tx);
    }
}


