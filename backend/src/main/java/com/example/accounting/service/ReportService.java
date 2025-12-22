package com.example.accounting.service;

import com.example.accounting.domain.Account;
import com.example.accounting.domain.Currency;
import com.example.accounting.domain.ExchangeRate;
import com.example.accounting.domain.Transaction;
import com.example.accounting.domain.TransactionEntry;
import com.example.accounting.domain.enums.AccountType;
import com.example.accounting.domain.enums.DebitCredit;
import com.example.accounting.repository.AccountRepository;
import com.example.accounting.repository.CurrencyRepository;
import com.example.accounting.repository.ExchangeRateRepository;
import com.example.accounting.repository.TransactionRepository;
import com.example.accounting.web.dto.report.BalanceSheetDto;
import com.example.accounting.web.dto.report.CashFlowStatementDto;
import com.example.accounting.web.dto.report.ProfitReportDto;
import com.example.accounting.web.dto.report.ReportRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportService {

    private final AccountRepository accountRepository;
    private final CurrencyRepository currencyRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final TransactionRepository transactionRepository;

    public ReportService(AccountRepository accountRepository,
                         CurrencyRepository currencyRepository,
                         ExchangeRateRepository exchangeRateRepository,
                         TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.currencyRepository = currencyRepository;
        this.exchangeRateRepository = exchangeRateRepository;
        this.transactionRepository = transactionRepository;
    }

    public ProfitReportDto generateProfitReport(ReportRequest request) {
        Currency reportCurrency = currencyRepository.findByCode(request.getCurrencyCode())
                .orElseThrow(() -> new IllegalArgumentException("报表币种不存在: " + request.getCurrencyCode()));

        List<Account> accounts = accountRepository.findAll();
        
        // 初始化明细列表
        List<ProfitReportDto.AccountItem> revenueItems = new ArrayList<>();
        List<ProfitReportDto.AccountItem> costItems = new ArrayList<>();
        List<ProfitReportDto.AccountItem> expenseItems = new ArrayList<>();
        
        BigDecimal operatingRevenue = BigDecimal.ZERO;
        BigDecimal operatingCost = BigDecimal.ZERO;
        BigDecimal sellingExpenses = BigDecimal.ZERO;
        BigDecimal administrativeExpenses = BigDecimal.ZERO;
        BigDecimal financialExpenses = BigDecimal.ZERO;

        for (Account acc : accounts) {
            BigDecimal bal = convert(acc.getBalance(), acc.getCurrency(), reportCurrency, request.getEndDate());
            if (bal.compareTo(BigDecimal.ZERO) == 0) {
                continue; // 跳过余额为0的账户
            }
            
            String name = acc.getName().toLowerCase();
            String code = acc.getCode() != null ? acc.getCode() : "";
            
            if (acc.getType() == AccountType.INCOME) {
                // 收入类账户
                operatingRevenue = operatingRevenue.add(bal);
                ProfitReportDto.AccountItem item = new ProfitReportDto.AccountItem();
                item.setAccountName(acc.getName());
                item.setAccountCode(code);
                item.setAmount(bal);
                revenueItems.add(item);
            } else if (acc.getType() == AccountType.EXPENSE) {
                // 费用类账户
                ProfitReportDto.AccountItem item = new ProfitReportDto.AccountItem();
                item.setAccountName(acc.getName());
                item.setAccountCode(code);
                item.setAmount(bal);
                
                // 根据账户名称分类费用
                if (name.contains("成本") || name.contains("cost") || name.contains("主营业务成本")) {
                    operatingCost = operatingCost.add(bal);
                    costItems.add(item);
                } else if (name.contains("销售") || name.contains("selling") || name.contains("销售费用")) {
                    sellingExpenses = sellingExpenses.add(bal);
                    expenseItems.add(item);
                } else if (name.contains("管理") || name.contains("administrative") || name.contains("管理费用")) {
                    administrativeExpenses = administrativeExpenses.add(bal);
                    expenseItems.add(item);
                } else if (name.contains("财务") || name.contains("financial") || name.contains("财务费用") 
                        || name.contains("利息")) {
                    financialExpenses = financialExpenses.add(bal);
                    expenseItems.add(item);
                } else {
                    // 其他费用归类为管理费用
                    administrativeExpenses = administrativeExpenses.add(bal);
                    expenseItems.add(item);
                }
            }
        }

        BigDecimal operatingExpenses = sellingExpenses.add(administrativeExpenses).add(financialExpenses);
        BigDecimal operatingProfit = operatingRevenue.subtract(operatingCost).subtract(operatingExpenses);
        BigDecimal totalProfit = operatingProfit; // 简化处理，假设没有营业外收支
        BigDecimal netProfit = totalProfit; // 简化处理，假设没有所得税

        ProfitReportDto dto = new ProfitReportDto();
        dto.setCurrencyCode(reportCurrency.getCode());
        dto.setOperatingRevenue(operatingRevenue);
        dto.setRevenueItems(revenueItems);
        dto.setOperatingCost(operatingCost);
        dto.setCostItems(costItems);
        dto.setOperatingExpenses(operatingExpenses);
        dto.setSellingExpenses(sellingExpenses);
        dto.setAdministrativeExpenses(administrativeExpenses);
        dto.setFinancialExpenses(financialExpenses);
        dto.setExpenseItems(expenseItems);
        dto.setOperatingProfit(operatingProfit);
        dto.setTotalProfit(totalProfit);
        dto.setNetProfit(netProfit);
        return dto;
    }

    public BalanceSheetDto generateBalanceSheet(ReportRequest request) {
        Currency reportCurrency = currencyRepository.findByCode(request.getCurrencyCode())
                .orElseThrow(() -> new IllegalArgumentException("报表币种不存在: " + request.getCurrencyCode()));

        List<Account> accounts = accountRepository.findAll();
        
        // 初始化明细列表
        List<BalanceSheetDto.AccountItem> currentAssetItems = new ArrayList<>();
        List<BalanceSheetDto.AccountItem> nonCurrentAssetItems = new ArrayList<>();
        List<BalanceSheetDto.AccountItem> currentLiabilityItems = new ArrayList<>();
        List<BalanceSheetDto.AccountItem> nonCurrentLiabilityItems = new ArrayList<>();
        List<BalanceSheetDto.AccountItem> equityItems = new ArrayList<>();
        
        // 资产
        BigDecimal cashAndEquivalents = BigDecimal.ZERO;
        BigDecimal accountsReceivable = BigDecimal.ZERO;
        BigDecimal inventory = BigDecimal.ZERO;
        BigDecimal prepaidExpenses = BigDecimal.ZERO;
        BigDecimal currentAssets = BigDecimal.ZERO;
        BigDecimal fixedAssets = BigDecimal.ZERO;
        BigDecimal longTermInvestments = BigDecimal.ZERO;
        BigDecimal nonCurrentAssets = BigDecimal.ZERO;
        
        // 负债
        BigDecimal accountsPayable = BigDecimal.ZERO;
        BigDecimal shortTermDebt = BigDecimal.ZERO;
        BigDecimal accruedExpenses = BigDecimal.ZERO;
        BigDecimal currentLiabilities = BigDecimal.ZERO;
        BigDecimal longTermDebt = BigDecimal.ZERO;
        BigDecimal nonCurrentLiabilities = BigDecimal.ZERO;
        
        // 所有者权益
        BigDecimal paidInCapital = BigDecimal.ZERO;
        BigDecimal retainedEarnings = BigDecimal.ZERO;
        BigDecimal equityTotal = BigDecimal.ZERO;

        for (Account acc : accounts) {
            BigDecimal bal = convert(acc.getBalance(), acc.getCurrency(), reportCurrency, request.getEndDate());
            if (bal.compareTo(BigDecimal.ZERO) == 0) {
                continue; // 跳过余额为0的账户
            }
            
            String name = acc.getName().toLowerCase();
            String code = acc.getCode() != null ? acc.getCode() : "";
            
            BalanceSheetDto.AccountItem item = new BalanceSheetDto.AccountItem();
            item.setAccountName(acc.getName());
            item.setAccountCode(code);
            item.setAmount(bal);
            
            if (acc.getType() == AccountType.ASSET) {
                // 资产类账户
                if (name.contains("现金") || name.contains("银行") || name.contains("cash") 
                        || name.contains("bank") || name.contains("货币资金")) {
                    cashAndEquivalents = cashAndEquivalents.add(bal);
                    currentAssetItems.add(item);
                } else if (name.contains("应收") || name.contains("receivable")) {
                    accountsReceivable = accountsReceivable.add(bal);
                    currentAssetItems.add(item);
                } else if (name.contains("存货") || name.contains("库存") || name.contains("inventory")) {
                    inventory = inventory.add(bal);
                    currentAssetItems.add(item);
                } else if (name.contains("预付") || name.contains("prepaid")) {
                    prepaidExpenses = prepaidExpenses.add(bal);
                    currentAssetItems.add(item);
                } else if (name.contains("固定") || name.contains("fixed") || name.contains("固定资产")) {
                    fixedAssets = fixedAssets.add(bal);
                    nonCurrentAssetItems.add(item);
                } else if (name.contains("投资") || name.contains("investment") || name.contains("长期投资")) {
                    longTermInvestments = longTermInvestments.add(bal);
                    nonCurrentAssetItems.add(item);
                } else {
                    // 其他资产，根据名称判断是流动资产还是非流动资产
                    // 简化处理：如果名称中没有明显的关键词，默认归类为流动资产
                    currentAssets = currentAssets.add(bal);
                    currentAssetItems.add(item);
                }
            } else if (acc.getType() == AccountType.LIABILITY) {
                // 负债类账户
                if (name.contains("应付") || name.contains("payable") || name.contains("应付账款")) {
                    accountsPayable = accountsPayable.add(bal);
                    currentLiabilityItems.add(item);
                } else if (name.contains("短期") || name.contains("short") || name.contains("短期借款")) {
                    shortTermDebt = shortTermDebt.add(bal);
                    currentLiabilityItems.add(item);
                } else if (name.contains("应付费用") || name.contains("accrued") || name.contains("预提")) {
                    accruedExpenses = accruedExpenses.add(bal);
                    currentLiabilityItems.add(item);
                } else if (name.contains("长期") || name.contains("long") || name.contains("长期借款")) {
                    longTermDebt = longTermDebt.add(bal);
                    nonCurrentLiabilityItems.add(item);
                } else {
                    // 其他负债，默认归类为流动负债
                    currentLiabilities = currentLiabilities.add(bal);
                    currentLiabilityItems.add(item);
                }
            } else if (acc.getType() == AccountType.EQUITY) {
                // 所有者权益类账户
                if (name.contains("实收") || name.contains("资本") || name.contains("paid") 
                        || name.contains("capital") || name.contains("股本")) {
                    paidInCapital = paidInCapital.add(bal);
                    equityItems.add(item);
                } else if (name.contains("留存") || name.contains("未分配") || name.contains("retained") 
                        || name.contains("盈余公积")) {
                    retainedEarnings = retainedEarnings.add(bal);
                    equityItems.add(item);
                } else {
                    // 其他权益，归类为留存收益
                    retainedEarnings = retainedEarnings.add(bal);
                    equityItems.add(item);
                }
            }
        }

        // 计算合计
        currentAssets = cashAndEquivalents.add(accountsReceivable).add(inventory).add(prepaidExpenses)
                .add(currentAssets); // 加上其他流动资产
        nonCurrentAssets = fixedAssets.add(longTermInvestments);
        BigDecimal assetTotal = currentAssets.add(nonCurrentAssets);
        
        currentLiabilities = accountsPayable.add(shortTermDebt).add(accruedExpenses)
                .add(currentLiabilities); // 加上其他流动负债
        nonCurrentLiabilities = longTermDebt.add(nonCurrentLiabilities); // 加上其他非流动负债
        BigDecimal liabilityTotal = currentLiabilities.add(nonCurrentLiabilities);
        
        equityTotal = paidInCapital.add(retainedEarnings);
        
        boolean balanced = assetTotal.compareTo(liabilityTotal.add(equityTotal)) == 0;

        BalanceSheetDto dto = new BalanceSheetDto();
        dto.setCurrencyCode(reportCurrency.getCode());
        dto.setCurrentAssets(currentAssets);
        dto.setCashAndEquivalents(cashAndEquivalents);
        dto.setAccountsReceivable(accountsReceivable);
        dto.setInventory(inventory);
        dto.setPrepaidExpenses(prepaidExpenses);
        dto.setCurrentAssetItems(currentAssetItems);
        dto.setNonCurrentAssets(nonCurrentAssets);
        dto.setFixedAssets(fixedAssets);
        dto.setLongTermInvestments(longTermInvestments);
        dto.setNonCurrentAssetItems(nonCurrentAssetItems);
        dto.setAssetTotal(assetTotal);
        dto.setCurrentLiabilities(currentLiabilities);
        dto.setAccountsPayable(accountsPayable);
        dto.setShortTermDebt(shortTermDebt);
        dto.setAccruedExpenses(accruedExpenses);
        dto.setCurrentLiabilityItems(currentLiabilityItems);
        dto.setNonCurrentLiabilities(nonCurrentLiabilities);
        dto.setLongTermDebt(longTermDebt);
        dto.setNonCurrentLiabilityItems(nonCurrentLiabilityItems);
        dto.setLiabilityTotal(liabilityTotal);
        dto.setEquityTotal(equityTotal);
        dto.setPaidInCapital(paidInCapital);
        dto.setRetainedEarnings(retainedEarnings);
        dto.setEquityItems(equityItems);
        dto.setBalanced(balanced);
        return dto;
    }

    public CashFlowStatementDto generateCashFlowStatement(ReportRequest request) {
        Currency reportCurrency = currencyRepository.findByCode(request.getCurrencyCode())
                .orElseThrow(() -> new IllegalArgumentException("报表币种不存在: " + request.getCurrencyCode()));

        // 识别现金账户（资产类账户，名称包含"现金"、"银行"等关键词）
        List<Account> cashAccounts = accountRepository.findAll().stream()
                .filter(acc -> acc.getType() == AccountType.ASSET
                        && (acc.getName().contains("现金") || acc.getName().contains("银行")
                        || acc.getName().toLowerCase().contains("cash") || acc.getName().toLowerCase().contains("bank")))
                .toList();

        // 查询期间内的所有交易
        List<Transaction> transactions = transactionRepository.findByDateBetween(
                request.getStartDate(), request.getEndDate());

        BigDecimal operatingInflow = BigDecimal.ZERO;
        BigDecimal operatingOutflow = BigDecimal.ZERO;
        BigDecimal investingInflow = BigDecimal.ZERO;
        BigDecimal investingOutflow = BigDecimal.ZERO;
        BigDecimal financingInflow = BigDecimal.ZERO;
        BigDecimal financingOutflow = BigDecimal.ZERO;

        // 计算期初和期末现金余额
        BigDecimal beginningCash = BigDecimal.ZERO;
        BigDecimal endingCash = BigDecimal.ZERO;

        // 计算期末现金余额
        for (Account cashAcc : cashAccounts) {
            // 简化处理：假设期初余额为0，实际应该查询期初日期之前的余额
            // 这里为了简化，我们使用账户当前余额减去期间内的变动来计算期初余额
            BigDecimal currentBalance = convert(cashAcc.getBalance(), cashAcc.getCurrency(), reportCurrency, request.getEndDate());
            endingCash = endingCash.add(currentBalance);
        }

        // 遍历所有交易，分析现金流量
        for (Transaction txn : transactions) {
            for (TransactionEntry entry : txn.getEntries()) {
                Account entryAccount = entry.getAccount();
                boolean isCashAccount = cashAccounts.contains(entryAccount);

                if (isCashAccount) {
                    // 这是现金账户的变动
                    BigDecimal amount = convert(entry.getAmount(), entryAccount.getCurrency(), reportCurrency, txn.getDate());
                    boolean isInflow = entry.getDebitCredit() == DebitCredit.DEBIT; // 现金账户借方为流入

                    // 找到对方账户，判断现金流量类型
                    Account counterpartAccount = findCounterpartAccount(txn, entryAccount);
                    CashFlowCategory category = classifyCashFlow(counterpartAccount);

                    if (isInflow) {
                        switch (category) {
                            case OPERATING -> operatingInflow = operatingInflow.add(amount);
                            case INVESTING -> investingInflow = investingInflow.add(amount);
                            case FINANCING -> financingInflow = financingInflow.add(amount);
                        }
                    } else {
                        switch (category) {
                            case OPERATING -> operatingOutflow = operatingOutflow.add(amount);
                            case INVESTING -> investingOutflow = investingOutflow.add(amount);
                            case FINANCING -> financingOutflow = financingOutflow.add(amount);
                        }
                    }
                }
            }
        }

        // 计算期初余额（期末余额减去期间净增加额）
        BigDecimal netIncrease = operatingInflow.subtract(operatingOutflow)
                .add(investingInflow.subtract(investingOutflow))
                .add(financingInflow.subtract(financingOutflow));
        beginningCash = endingCash.subtract(netIncrease);

        CashFlowStatementDto dto = new CashFlowStatementDto();
        dto.setCurrencyCode(reportCurrency.getCode());
        dto.setOperatingCashInflow(operatingInflow);
        dto.setOperatingCashOutflow(operatingOutflow);
        dto.setOperatingNetCashFlow(operatingInflow.subtract(operatingOutflow));
        dto.setInvestingCashInflow(investingInflow);
        dto.setInvestingCashOutflow(investingOutflow);
        dto.setInvestingNetCashFlow(investingInflow.subtract(investingOutflow));
        dto.setFinancingCashInflow(financingInflow);
        dto.setFinancingCashOutflow(financingOutflow);
        dto.setFinancingNetCashFlow(financingInflow.subtract(financingOutflow));
        dto.setNetIncreaseInCash(netIncrease);
        dto.setBeginningCashBalance(beginningCash);
        dto.setEndingCashBalance(endingCash);

        return dto;
    }

    /**
     * 找到交易中的对方账户（非现金账户）
     */
    private Account findCounterpartAccount(Transaction txn, Account cashAccount) {
        for (TransactionEntry entry : txn.getEntries()) {
            if (!entry.getAccount().equals(cashAccount)) {
                return entry.getAccount();
            }
        }
        return null; // 如果找不到，返回null
    }

    /**
     * 根据账户类型判断现金流量类别
     */
    private CashFlowCategory classifyCashFlow(Account account) {
        if (account == null) {
            return CashFlowCategory.OPERATING; // 默认归类为经营活动
        }

        AccountType type = account.getType();
        String name = account.getName().toLowerCase();

        // 投资活动：固定资产、长期投资等
        if (type == AccountType.ASSET && (name.contains("固定") || name.contains("投资")
                || name.contains("fixed") || name.contains("investment"))) {
            return CashFlowCategory.INVESTING;
        }

        // 筹资活动：负债、权益类账户
        if (type == AccountType.LIABILITY || type == AccountType.EQUITY) {
            return CashFlowCategory.FINANCING;
        }

        // 经营活动：收入、费用、其他资产（如应收账款、存货等）
        return CashFlowCategory.OPERATING;
    }

    /**
     * 现金流量类别枚举
     */
    private enum CashFlowCategory {
        OPERATING,  // 经营活动
        INVESTING,   // 投资活动
        FINANCING   // 筹资活动
    }

    private BigDecimal convert(BigDecimal amount, Currency from, Currency to, LocalDate date) {
        if (from.getId().equals(to.getId()) || amount.compareTo(BigDecimal.ZERO) == 0) {
            return amount;
        }
        ExchangeRate rate = exchangeRateRepository
                .findTopByFromCurrencyAndToCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(from, to, date)
                .orElseThrow(() -> new IllegalArgumentException(
                        "缺少从 " + from.getCode() + " 到 " + to.getCode() + " 的汇率"));
        return amount.multiply(rate.getRate());
    }
}


