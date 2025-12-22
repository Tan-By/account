package com.example.accounting.service;

import com.example.accounting.domain.*;
import com.example.accounting.repository.*;
import com.example.accounting.web.dto.reconciliation.ReconciliationRequest;
import com.example.accounting.web.dto.reconciliation.ReconciliationResultDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ReconciliationService {

    private final AccountRepository accountRepository;
    private final BankStatementRecordRepository bankStatementRecordRepository;
    private final TransactionEntryRepository transactionEntryRepository;
    private final ReconciliationTaskRepository reconciliationTaskRepository;
    private final CurrencyRepository currencyRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    public ReconciliationService(AccountRepository accountRepository,
                                 BankStatementRecordRepository bankStatementRecordRepository,
                                 TransactionEntryRepository transactionEntryRepository,
                                 ReconciliationTaskRepository reconciliationTaskRepository,
                                 CurrencyRepository currencyRepository,
                                 ExchangeRateRepository exchangeRateRepository) {
        this.accountRepository = accountRepository;
        this.bankStatementRecordRepository = bankStatementRecordRepository;
        this.transactionEntryRepository = transactionEntryRepository;
        this.reconciliationTaskRepository = reconciliationTaskRepository;
        this.currencyRepository = currencyRepository;
        this.exchangeRateRepository = exchangeRateRepository;
    }

    @Transactional
    public ReconciliationResultDto reconcile(ReconciliationRequest request) {
        Account bankAccount = accountRepository.findById(request.getBankAccountId())
                .orElseThrow(() -> new IllegalArgumentException("银行账户不存在"));

        List<BankStatementRecord> bankRecords =
                bankStatementRecordRepository.findByBankAccountAndDateBetween(
                        bankAccount, request.getPeriodStart(), request.getPeriodEnd());

        List<TransactionEntry> bookEntries =
                transactionEntryRepository.findByAccountAndTransaction_DateBetween(
                        bankAccount, request.getPeriodStart(), request.getPeriodEnd());

        // 简化版匹配规则：金额+日期均一致则视为匹配
        Set<Long> matchedBankIds = new HashSet<>();
        Set<Long> matchedBookIds = new HashSet<>();

        for (BankStatementRecord br : bankRecords) {
            BigDecimal bankAmount = br.getDebitAmount() != null ? br.getDebitAmount()
                    : br.getCreditAmount() != null ? br.getCreditAmount() : BigDecimal.ZERO;
            for (TransactionEntry te : bookEntries) {
                if (matchedBookIds.contains(te.getId())) {
                    continue;
                }
                BigDecimal bookAmount = te.getAmount();
                if (br.getDate().equals(te.getTransaction().getDate())
                        && bankAmount.compareTo(bookAmount) == 0) {
                    matchedBankIds.add(br.getId());
                    matchedBookIds.add(te.getId());
                    br.setMatchStatus("已匹配");
                    bankStatementRecordRepository.save(br);
                    break;
                }
            }
        }

        int matchedCount = matchedBankIds.size();
        int unmatchedBankCount = bankRecords.size() - matchedCount;
        int unmatchedBookCount = bookEntries.size() - matchedBookIds.size();

        BigDecimal bankBalance = bankAccount.getBalance(); // 简化：直接使用账面余额作为银行余额
        BigDecimal bookBalance = bankAccount.getBalance();
        
        // 如果指定了显示币种，则转换余额
        Currency displayCurr = null;
        if (request.getDisplayCurrency() != null && !request.getDisplayCurrency().isEmpty()) {
            displayCurr = currencyRepository.findByCode(request.getDisplayCurrency()).orElse(null);
        }
        if (displayCurr != null && !bankAccount.getCurrency().getId().equals(displayCurr.getId())) {
            LocalDate today = LocalDate.now();
            bankBalance = convert(bankBalance, bankAccount.getCurrency(), displayCurr, today);
            bookBalance = convert(bookBalance, bankAccount.getCurrency(), displayCurr, today);
        }

        // 未达账项汇总（简化：只按数量统计，不细分四种类型）
        BigDecimal diff = bankBalance.subtract(bookBalance);

        ReconciliationTask task = new ReconciliationTask();
        task.setBankAccount(bankAccount);
        task.setPeriodStart(request.getPeriodStart());
        task.setPeriodEnd(request.getPeriodEnd());
        task.setStatus("已完成");
        task.setExecutor(request.getExecutor());
        task.setExecutedAt(LocalDateTime.now());
        reconciliationTaskRepository.save(task);

        ReconciliationResultDto result = new ReconciliationResultDto();
        result.setTaskId(task.getId());
        result.setMatchedCount(matchedCount);
        result.setUnmatchedBankCount(unmatchedBankCount);
        result.setUnmatchedBookCount(unmatchedBookCount);
        result.setBookBalance(bookBalance);
        result.setBankBalance(bankBalance);
        result.setAdjustedBookBalance(bookBalance.add(diff));
        result.setAdjustedBankBalance(bankBalance);
        return result;
    }
    
    private BigDecimal convert(BigDecimal amount, Currency from, Currency to, LocalDate date) {
        if (from.getId().equals(to.getId()) || amount.compareTo(BigDecimal.ZERO) == 0) {
            return amount;
        }
        Optional<ExchangeRate> rateOpt = exchangeRateRepository
                .findTopByFromCurrencyAndToCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(from, to, date);
        if (rateOpt.isEmpty()) {
            return amount;
        }
        return amount.multiply(rateOpt.get().getRate());
    }
}


