package com.example.accounting.web.dto.purchase;

import com.example.accounting.domain.enums.PurchaseOrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class PurchaseOrderDto {
    private Long id;
    private String orderNumber;
    private Long supplierId;
    private String supplierName;
    private LocalDate orderDate;
    private LocalDate deliveryDate;
    private Long warehouseId;
    private String warehouseName;
    private BigDecimal totalAmount;
    private PurchaseOrderStatus status;
    private Long submitterId;
    private String submitterName;
    private LocalDate submitTime;
    private Long approverId;
    private String approverName;
    private LocalDate approvalTime;
    private String approvalComment;
    private List<PurchaseOrderItemDto> items;
}

