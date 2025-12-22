package com.example.accounting.web.dto.tax;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaxDeclarationRequest {

    @NotBlank
    private String taxType;

    @NotNull
    private LocalDate periodStart;

    @NotNull
    private LocalDate periodEnd;
}


