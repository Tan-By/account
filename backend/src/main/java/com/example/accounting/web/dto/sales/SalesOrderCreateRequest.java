package com.example.accounting.web.dto.sales;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class SalesOrderCreateRequest {
    @NotNull(message = "客户不能为空")
    private Long customerId;
    
    @NotNull(message = "下单日期不能为空")
    private LocalDate orderDate;
    
    @NotNull(message = "交货日期不能为空")
    private LocalDate deliveryDate;
    
    private String shippingAddress;
    private String paymentMethod;
    
    @Valid
    @NotNull(message = "订单明细不能为空")
    private List<SalesOrderItemCreateRequest> items;
}

