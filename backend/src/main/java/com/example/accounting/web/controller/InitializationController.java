package com.example.accounting.web.controller;

import com.example.accounting.domain.Company;
import com.example.accounting.service.InitializationService;
import com.example.accounting.web.dto.init.InitializationRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/init")
@CrossOrigin
public class InitializationController {

    private final InitializationService initializationService;

    public InitializationController(InitializationService initializationService) {
        this.initializationService = initializationService;
    }

    @PostMapping
    public ResponseEntity<Company> initialize(@Valid @RequestBody InitializationRequest request) {
        Company company = initializationService.initialize(request);
        return ResponseEntity.ok(company);
    }
}


