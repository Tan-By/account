package com.example.accounting.web.dto.purchase;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PurchaseOrderItemDto {
    private Long id;
    private Long productId;
    private String productCode;
    private String productName;
    private String specification;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private BigDecimal deliveredQuantity;
    private BigDecimal undeliveredQuantity;
}

