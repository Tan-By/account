package com.example.accounting.web.dto.reconciliation;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReconciliationResultDto {

    private Long taskId;

    private int matchedCount;

    private int unmatchedBankCount;

    private int unmatchedBookCount;

    private BigDecimal bookBalance;

    private BigDecimal bankBalance;

    private BigDecimal adjustedBookBalance;

    private BigDecimal adjustedBankBalance;

    /**
     * 银行流水明细（带匹配状态）
     */
    private java.util.List<BankRecordDto> bankRecords;

    /**
     * 账面分录明细（带匹配状态）
     */
    private java.util.List<BookEntryDto> bookEntries;
}


