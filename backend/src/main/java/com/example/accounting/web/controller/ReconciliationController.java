package com.example.accounting.web.controller;

import com.example.accounting.service.ReconciliationService;
import com.example.accounting.web.dto.reconciliation.ReconciliationRequest;
import com.example.accounting.web.dto.reconciliation.ReconciliationResultDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reconciliations")
@CrossOrigin
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @PostMapping
    public ResponseEntity<ReconciliationResultDto> reconcile(@Valid @RequestBody ReconciliationRequest request) {
        return ResponseEntity.ok(reconciliationService.reconcile(request));
    }
}


