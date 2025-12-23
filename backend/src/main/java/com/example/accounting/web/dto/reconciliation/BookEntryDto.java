package com.example.accounting.web.dto.reconciliation;

import com.example.accounting.domain.enums.DebitCredit;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BookEntryDto {
    private Long id;
    private Long transactionId;
    private LocalDate date;
    private String description;
    private DebitCredit debitCredit;
    private BigDecimal amount;
    private String voucherStatus;
    private String matchStatus; // 已匹配 | 未匹配
}


