package com.example.accounting.web.dto.sales;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequest {
    @NotNull(message = "收款金额不能为空")
    @Positive(message = "收款金额必须大于0")
    private BigDecimal amount;
}

