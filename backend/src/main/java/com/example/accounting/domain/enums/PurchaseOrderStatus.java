package com.example.accounting.domain.enums;

/**
 * 订货单状态枚举
 */
public enum PurchaseOrderStatus {
    /**
     * 草稿
     */
    DRAFT,
    
    /**
     * 待审批
     */
    PENDING_APPROVAL,
    
    /**
     * 已批准
     */
    APPROVED,
    
    /**
     * 已拒绝
     */
    REJECTED,
    
    /**
     * 部分交货
     */
    PARTIAL_DELIVERY,
    
    /**
     * 已完成
     */
    COMPLETED,
    
    /**
     * 已取消
     */
    CANCELLED
}

