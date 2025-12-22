package com.example.accounting.web.dto.report;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 资产负债表DTO - 详细版
 * 按照会计准则，资产负债表分为：
 * 1. 资产（流动资产、非流动资产）
 * 2. 负债（流动负债、非流动负债）
 * 3. 所有者权益
 */
@Data
public class BalanceSheetDto {

    private String currencyCode;

    /**
     * 一、资产
     */
    // 流动资产
    private BigDecimal currentAssets;         // 流动资产合计
    private BigDecimal cashAndEquivalents;    // 现金及现金等价物
    private BigDecimal accountsReceivable;     // 应收账款
    private BigDecimal inventory;             // 存货
    private BigDecimal prepaidExpenses;       // 预付账款
    private List<AccountItem> currentAssetItems; // 流动资产明细

    // 非流动资产
    private BigDecimal nonCurrentAssets;       // 非流动资产合计
    private BigDecimal fixedAssets;           // 固定资产
    private BigDecimal longTermInvestments;   // 长期投资
    private List<AccountItem> nonCurrentAssetItems; // 非流动资产明细

    private BigDecimal assetTotal;            // 资产总计

    /**
     * 二、负债
     */
    // 流动负债
    private BigDecimal currentLiabilities;    // 流动负债合计
    private BigDecimal accountsPayable;       // 应付账款
    private BigDecimal shortTermDebt;         // 短期借款
    private BigDecimal accruedExpenses;       // 应付费用
    private List<AccountItem> currentLiabilityItems; // 流动负债明细

    // 非流动负债
    private BigDecimal nonCurrentLiabilities; // 非流动负债合计
    private BigDecimal longTermDebt;         // 长期借款
    private List<AccountItem> nonCurrentLiabilityItems; // 非流动负债明细

    private BigDecimal liabilityTotal;        // 负债总计

    /**
     * 三、所有者权益
     */
    private BigDecimal equityTotal;            // 所有者权益总计
    private BigDecimal paidInCapital;         // 实收资本
    private BigDecimal retainedEarnings;      // 留存收益
    private List<AccountItem> equityItems;     // 所有者权益明细

    /**
     * 平衡校验
     */
    private boolean balanced;                  // 资产 = 负债 + 所有者权益

    /**
     * 账户明细项
     */
    @Data
    public static class AccountItem {
        private String accountName;            // 账户名称
        private String accountCode;            // 账户编码
        private BigDecimal amount;             // 金额
    }
}


