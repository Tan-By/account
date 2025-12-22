package com.example.accounting.web.dto.report;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProfitReportDto {

    private String currencyCode;

    private BigDecimal incomeTotal;

    private BigDecimal expenseTotal;

    private BigDecimal netProfit;
}


