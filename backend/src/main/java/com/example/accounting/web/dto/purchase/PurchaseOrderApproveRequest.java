package com.example.accounting.web.dto.purchase;

import lombok.Data;

@Data
public class PurchaseOrderApproveRequest {
    private boolean approved;
    private String comment;
}

