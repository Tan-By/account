package com.example.accounting.web.dto.sales;

import com.example.accounting.domain.enums.SalesOrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class SalesOrderDto {
    private Long id;
    private String orderNumber;
    private Long customerId;
    private String customerName;
    private LocalDate orderDate;
    private LocalDate deliveryDate;
    private String shippingAddress;
    private String paymentMethod;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal receivableAmount;
    private BigDecimal receivedAmount;
    private BigDecimal unreceivedAmount;
    private String invoiceStatus;
    private SalesOrderStatus status;
    private Long submitterId;
    private String submitterName;
    private LocalDate submitTime;
    private Long auditorId;
    private String auditorName;
    private LocalDate auditTime;
    private String auditComment;
    private List<SalesOrderItemDto> items;
}

