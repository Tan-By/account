package com.example.accounting.web.dto.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ReportRequest {

    /**
     * 报表类型：PROFIT 或 BALANCE_SHEET
     */
    @NotBlank
    private String type;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    /**
     * 报表币种代码（如 CNY），用于多币种折算
     */
    @NotBlank
    private String currencyCode;
}


