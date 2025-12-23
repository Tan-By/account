package com.example.accounting.web.dto.sales;

import lombok.Data;

@Data
public class SalesOrderAuditRequest {
    private boolean approved;
    private String comment;
}

