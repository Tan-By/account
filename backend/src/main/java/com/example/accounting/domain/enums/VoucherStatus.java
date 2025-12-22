package com.example.accounting.domain.enums;

/**
 * 凭证状态枚举
 * 未审核：凭证已保存但未审核
 * 已审核：凭证已审核，可以过账
 * 已过账：凭证已过账，不能修改或删除
 */
public enum VoucherStatus {
    /**
     * 未审核（已保存/待审核）
     */
    UNAUDITED,
    
    /**
     * 已审核
     */
    AUDITED,
    
    /**
     * 已过账
     */
    POSTED
}


