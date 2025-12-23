package com.example.accounting.web.dto.reconciliation;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BankRecordDto {
    private Long id;
    private LocalDate date;
    private String description;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private BigDecimal balance;
    private String reference;
    private String matchStatus; // 已匹配 | 未匹配 | 待确认
}


