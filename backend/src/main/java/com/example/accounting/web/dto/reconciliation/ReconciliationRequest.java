package com.example.accounting.web.dto.reconciliation;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ReconciliationRequest {

    @NotNull
    private Long bankAccountId;

    @NotNull
    private LocalDate periodStart;

    @NotNull
    private LocalDate periodEnd;

    private String executor;
    
    private String displayCurrency;
}


