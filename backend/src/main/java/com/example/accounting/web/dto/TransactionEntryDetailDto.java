package com.example.accounting.web.dto;

import com.example.accounting.domain.enums.DebitCredit;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionEntryDetailDto {
    private Long id;
    private Long accountId;
    private String accountName;
    private String currencyCode;
    private DebitCredit debitCredit;
    private BigDecimal amount;
}

