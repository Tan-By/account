package com.example.accounting.web.controller;

import com.example.accounting.domain.TaxDeclaration;
import com.example.accounting.repository.TaxDeclarationRepository;
import com.example.accounting.service.TaxDeclarationService;
import com.example.accounting.web.dto.tax.TaxDeclarationRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tax-declarations")
@CrossOrigin
public class TaxDeclarationController {

    private final TaxDeclarationRepository repository;
    private final TaxDeclarationService service;

    public TaxDeclarationController(TaxDeclarationRepository repository,
                                    TaxDeclarationService service) {
        this.repository = repository;
        this.service = service;
    }

    @GetMapping
    public List<TaxDeclaration> list() {
        return repository.findAll();
    }

    @PostMapping("/draft")
    public ResponseEntity<TaxDeclaration> createDraft(@Valid @RequestBody TaxDeclarationRequest request) {
        return ResponseEntity.ok(service.createDraft(request));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<TaxDeclaration> submit(@PathVariable Long id) {
        return ResponseEntity.ok(service.submit(id));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<TaxDeclaration> accept(@PathVariable Long id) {
        return ResponseEntity.ok(service.markAccepted(id));
    }

    @PostMapping("/{id}/success")
    public ResponseEntity<TaxDeclaration> success(@PathVariable Long id) {
        return ResponseEntity.ok(service.markSuccess(id));
    }
}


