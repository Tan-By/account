package com.example.accounting.domain.enums;

/**
 * 销售单状态枚举
 */
public enum SalesOrderStatus {
    /**
     * 草稿
     */
    DRAFT,
    
    /**
     * 待审核
     */
    PENDING_AUDIT,
    
    /**
     * 已审核
     */
    AUDITED,
    
    /**
     * 已拒绝
     */
    REJECTED,
    
    /**
     * 部分发货
     */
    PARTIAL_SHIPMENT,
    
    /**
     * 已发货
     */
    SHIPPED,
    
    /**
     * 部分收款
     */
    PARTIAL_PAYMENT,
    
    /**
     * 已完成
     */
    COMPLETED,
    
    /**
     * 已取消
     */
    CANCELLED
}

