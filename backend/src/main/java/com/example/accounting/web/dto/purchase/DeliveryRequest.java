package com.example.accounting.web.dto.purchase;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DeliveryRequest {
    @NotNull(message = "商品明细ID不能为空")
    private Long itemId;
    
    @NotNull(message = "交货数量不能为空")
    @Positive(message = "交货数量必须大于0")
    private BigDecimal quantity;
}

