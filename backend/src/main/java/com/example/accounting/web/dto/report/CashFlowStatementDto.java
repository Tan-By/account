package com.example.accounting.web.dto.report;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 现金流量表DTO
 * 按照会计准则，现金流量表分为三个部分：
 * 1. 经营活动产生的现金流量
 * 2. 投资活动产生的现金流量
 * 3. 筹资活动产生的现金流量
 */
@Data
public class CashFlowStatementDto {

    private String currencyCode;

    /**
     * 一、经营活动产生的现金流量
     */
    private BigDecimal operatingCashInflow;      // 经营活动现金流入
    private BigDecimal operatingCashOutflow;    // 经营活动现金流出
    private BigDecimal operatingNetCashFlow;     // 经营活动产生的现金流量净额

    /**
     * 二、投资活动产生的现金流量
     */
    private BigDecimal investingCashInflow;     // 投资活动现金流入
    private BigDecimal investingCashOutflow;    // 投资活动现金流出
    private BigDecimal investingNetCashFlow;    // 投资活动产生的现金流量净额

    /**
     * 三、筹资活动产生的现金流量
     */
    private BigDecimal financingCashInflow;     // 筹资活动现金流入
    private BigDecimal financingCashOutflow;    // 筹资活动现金流出
    private BigDecimal financingNetCashFlow;   // 筹资活动产生的现金流量净额

    /**
     * 四、现金及现金等价物净增加额
     */
    private BigDecimal netIncreaseInCash;

    /**
     * 五、期初现金及现金等价物余额
     */
    private BigDecimal beginningCashBalance;

    /**
     * 六、期末现金及现金等价物余额
     */
    private BigDecimal endingCashBalance;
}







