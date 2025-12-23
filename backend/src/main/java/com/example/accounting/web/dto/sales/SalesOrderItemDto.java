package com.example.accounting.web.dto.sales;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalesOrderItemDto {
    private Long id;
    private Long productId;
    private String productCode;
    private String productName;
    private String specification;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountRate;
    private BigDecimal amount;
    private BigDecimal shippedQuantity;
    private BigDecimal unshippedQuantity;
}

