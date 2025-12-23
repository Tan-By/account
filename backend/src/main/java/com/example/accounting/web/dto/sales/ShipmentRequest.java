package com.example.accounting.web.dto.sales;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ShipmentRequest {
    @NotNull(message = "商品明细ID不能为空")
    private Long itemId;
    
    @NotNull(message = "发货数量不能为空")
    @Positive(message = "发货数量必须大于0")
    private BigDecimal quantity;
}

