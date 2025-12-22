package com.example.accounting.service;

import com.example.accounting.domain.Account;
import com.example.accounting.domain.Currency;
import com.example.accounting.domain.ExchangeRate;
import com.example.accounting.domain.enums.AccountType;
import com.example.accounting.repository.AccountRepository;
import com.example.accounting.repository.CurrencyRepository;
import com.example.accounting.repository.ExchangeRateRepository;
import com.example.accounting.web.dto.report.BalanceSheetDto;
import com.example.accounting.web.dto.report.ProfitReportDto;
import com.example.accounting.web.dto.report.ReportRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class ReportService {

    private final AccountRepository accountRepository;
    private final CurrencyRepository currencyRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    public ReportService(AccountRepository accountRepository,
                         CurrencyRepository currencyRepository,
                         ExchangeRateRepository exchangeRateRepository) {
        this.accountRepository = accountRepository;
        this.currencyRepository = currencyRepository;
        this.exchangeRateRepository = exchangeRateRepository;
    }

    public ProfitReportDto generateProfitReport(ReportRequest request) {
        Currency reportCurrency = currencyRepository.findByCode(request.getCurrencyCode())
                .orElseThrow(() -> new IllegalArgumentException("报表币种不存在: " + request.getCurrencyCode()));

        List<Account> accounts = accountRepository.findAll();
        BigDecimal incomeTotal = BigDecimal.ZERO;
        BigDecimal expenseTotal = BigDecimal.ZERO;

        for (Account acc : accounts) {
            BigDecimal bal = convert(acc.getBalance(), acc.getCurrency(), reportCurrency, request.getEndDate());
            if (acc.getType() == AccountType.INCOME) {
                // 简化：收入类账户余额为贷方余额，代表正的收入
                incomeTotal = incomeTotal.add(bal);
            } else if (acc.getType() == AccountType.EXPENSE) {
                expenseTotal = expenseTotal.add(bal);
            }
        }

        ProfitReportDto dto = new ProfitReportDto();
        dto.setCurrencyCode(reportCurrency.getCode());
        dto.setIncomeTotal(incomeTotal);
        dto.setExpenseTotal(expenseTotal);
        dto.setNetProfit(incomeTotal.subtract(expenseTotal));
        return dto;
    }

    public BalanceSheetDto generateBalanceSheet(ReportRequest request) {
        Currency reportCurrency = currencyRepository.findByCode(request.getCurrencyCode())
                .orElseThrow(() -> new IllegalArgumentException("报表币种不存在: " + request.getCurrencyCode()));

        List<Account> accounts = accountRepository.findAll();
        BigDecimal asset = BigDecimal.ZERO;
        BigDecimal liability = BigDecimal.ZERO;
        BigDecimal equity = BigDecimal.ZERO;

        for (Account acc : accounts) {
            BigDecimal bal = convert(acc.getBalance(), acc.getCurrency(), reportCurrency, request.getEndDate());
            if (acc.getType() == AccountType.ASSET) {
                asset = asset.add(bal);
            } else if (acc.getType() == AccountType.LIABILITY) {
                liability = liability.add(bal);
            } else if (acc.getType() == AccountType.EQUITY) {
                equity = equity.add(bal);
            }
        }

        BalanceSheetDto dto = new BalanceSheetDto();
        dto.setCurrencyCode(reportCurrency.getCode());
        dto.setAssetTotal(asset);
        dto.setLiabilityTotal(liability);
        dto.setEquityTotal(equity);
        dto.setBalanced(asset.compareTo(liability.add(equity)) == 0);
        return dto;
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


