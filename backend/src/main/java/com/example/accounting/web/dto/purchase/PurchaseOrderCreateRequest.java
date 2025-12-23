package com.example.accounting.web.dto.purchase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PurchaseOrderCreateRequest {
    @NotNull(message = "供货商不能为空")
    private Long supplierId;
    
    @NotNull(message = "订单日期不能为空")
    private LocalDate orderDate;
    
    @NotNull(message = "交货日期不能为空")
    private LocalDate deliveryDate;
    
    private Long warehouseId;
    
    @Valid
    @NotNull(message = "订单明细不能为空")
    private List<PurchaseOrderItemCreateRequest> items;
}

