package com.example.accounting.web.dto.purchase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PurchaseOrderItemCreateRequest {
    /**
     * 商品ID（可选，如果提供则使用商品信息，否则使用手动输入的信息）
     */
    private Long productId;
    
    /**
     * 商品名称（手动输入，必填）
     */
    @NotBlank(message = "商品名称不能为空")
    private String productName;
    
    /**
     * 商品单位（手动输入，必填）
     */
    @NotBlank(message = "商品单位不能为空")
    private String unit;
    
    /**
     * 订购数量（整数，必填）
     */
    @NotNull(message = "订购数量不能为空")
    @Positive(message = "订购数量必须大于0")
    private Integer quantity;
    
    /**
     * 单价（必填）
     */
    @NotNull(message = "单价不能为空")
    @Positive(message = "单价必须大于0")
    private BigDecimal unitPrice;
}

