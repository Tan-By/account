package com.example.accounting.web.controller;

import com.example.accounting.domain.Transaction;
import com.example.accounting.service.TransactionService;
import com.example.accounting.web.dto.TransactionCreateRequest;
import com.example.accounting.web.dto.TransactionDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transactions")
@CrossOrigin
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public List<TransactionDto> list(@RequestParam(required = false) String displayCurrency) {
        return transactionService.listAll(displayCurrency);
    }

    @PostMapping
    public ResponseEntity<Transaction> create(@Valid @RequestBody TransactionCreateRequest request) {
        Transaction tx = transactionService.create(request);
        return ResponseEntity.ok(tx);
    }

    /**
     * 审核凭证（将状态从"未审核"改为"已审核"）
     */
    @PostMapping("/{id}/audit")
    public ResponseEntity<Transaction> audit(@PathVariable Long id) {
        Transaction tx = transactionService.audit(id);
        return ResponseEntity.ok(tx);
    }
}


