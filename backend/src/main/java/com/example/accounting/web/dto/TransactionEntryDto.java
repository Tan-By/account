package com.example.accounting.web.dto;

import com.example.accounting.domain.enums.DebitCredit;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionEntryDto {

    @NotNull
    private Long accountId;

    @NotNull
    private DebitCredit debitCredit;

    @NotNull
    private BigDecimal amount;
}


