package com.example.accounting.web.dto.report;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BalanceSheetDto {

    private String currencyCode;

    private BigDecimal assetTotal;

    private BigDecimal liabilityTotal;

    private BigDecimal equityTotal;

    private boolean balanced;
}


