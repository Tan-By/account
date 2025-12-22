package com.example.accounting.web.dto.report;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 利润表DTO - 详细版
 * 按照会计准则，利润表包含：
 * 1. 营业收入
 * 2. 营业成本
 * 3. 营业利润
 * 4. 利润总额
 * 5. 净利润
 */
@Data
public class ProfitReportDto {

    private String currencyCode;

    /**
     * 一、营业收入
     */
    private BigDecimal operatingRevenue;        // 营业收入
    private List<AccountItem> revenueItems;     // 收入明细项

    /**
     * 二、营业成本
     */
    private BigDecimal operatingCost;          // 营业成本
    private List<AccountItem> costItems;       // 成本明细项

    /**
     * 三、营业费用
     */
    private BigDecimal operatingExpenses;       // 营业费用合计
    private BigDecimal sellingExpenses;        // 销售费用
    private BigDecimal administrativeExpenses;  // 管理费用
    private BigDecimal financialExpenses;      // 财务费用
    private List<AccountItem> expenseItems;     // 费用明细项

    /**
     * 四、营业利润
     */
    private BigDecimal operatingProfit;        // 营业利润 = 营业收入 - 营业成本 - 营业费用

    /**
     * 五、利润总额
     */
    private BigDecimal totalProfit;            // 利润总额（简化处理，等于营业利润）

    /**
     * 六、净利润
     */
    private BigDecimal netProfit;              // 净利润 = 利润总额

    /**
     * 账户明细项（用于展示具体账户）
     */
    @Data
    public static class AccountItem {
        private String accountName;            // 账户名称
        private String accountCode;            // 账户编码
        private BigDecimal amount;           // 金额
    }
}


