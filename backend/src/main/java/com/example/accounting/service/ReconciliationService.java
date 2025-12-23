package com.example.accounting.service;

import com.example.accounting.domain.*;
import com.example.accounting.repository.*;
import com.example.accounting.domain.enums.VoucherStatus;
import com.example.accounting.web.dto.reconciliation.BankRecordDto;
import com.example.accounting.web.dto.reconciliation.BookEntryDto;
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

        // 银行余额：若流水有 balance 字段则取最新一条，否则回退到账面余额
        BigDecimal bankBalance = bankAccount.getBalance();
        if (!bankRecords.isEmpty()) {
            BankStatementRecord latest = bankRecords.stream()
                    .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                    .findFirst()
                    .orElse(null);
            if (latest != null && latest.getBalance() != null) {
                bankBalance = latest.getBalance();
            }
        }
        BigDecimal bookBalance = bankAccount.getBalance();
        
        // 如果指定了显示币种，则转换余额
        Currency displayCurr = null;
        if (request.getDisplayCurrency() != null && !request.getDisplayCurrency().isEmpty()) {
            displayCurr = currencyRepository.findByCode(request.getDisplayCurrency()).orElse(null);
        }
        final Currency displayCurrFinal = displayCurr;
        if (displayCurr != null && !bankAccount.getCurrency().getId().equals(displayCurr.getId())) {
            LocalDate today = LocalDate.now();
            bankBalance = convert(bankBalance, bankAccount.getCurrency(), displayCurr, today);
            bookBalance = convert(bookBalance, bankAccount.getCurrency(), displayCurr, today);
        }

        // 未达账项汇总（简化：不细分类型，仅以余额差额体现）
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

        // 构建银行流水明细 DTO
        result.setBankRecords(
                bankRecords.stream().map(br -> {
                    BankRecordDto dto = new BankRecordDto();
                    dto.setId(br.getId());
                    dto.setDate(br.getDate());
                    dto.setDescription(br.getDescription());
                    dto.setReference(br.getReference());
                    dto.setMatchStatus(matchedBankIds.contains(br.getId()) ? "已匹配" : "未匹配");

                    BigDecimal debit = br.getDebitAmount();
                    BigDecimal credit = br.getCreditAmount();
                    BigDecimal balanceVal = br.getBalance();
                    if (displayCurrFinal != null && !bankAccount.getCurrency().getId().equals(displayCurrFinal.getId())) {
                        LocalDate today = LocalDate.now();
                        if (debit != null) {
                            debit = convert(debit, bankAccount.getCurrency(), displayCurrFinal, today);
                        }
                        if (credit != null) {
                            credit = convert(credit, bankAccount.getCurrency(), displayCurrFinal, today);
                        }
                        if (balanceVal != null) {
                            balanceVal = convert(balanceVal, bankAccount.getCurrency(), displayCurrFinal, today);
                        }
                    }
                    dto.setDebitAmount(debit);
                    dto.setCreditAmount(credit);
                    dto.setBalance(balanceVal);
                    return dto;
                }).toList()
        );

        // 构建账面分录明细 DTO
        result.setBookEntries(
                bookEntries.stream().map(te -> {
                    BookEntryDto dto = new BookEntryDto();
                    dto.setId(te.getId());
                    dto.setTransactionId(te.getTransaction().getId());
                    dto.setDate(te.getTransaction().getDate());
                    dto.setDescription(te.getTransaction().getDescription());
                    dto.setDebitCredit(te.getDebitCredit());
                    dto.setVoucherStatus(Optional.ofNullable(te.getTransaction().getStatus()).map(VoucherStatus::name).orElse(null));
                    dto.setMatchStatus(matchedBookIds.contains(te.getId()) ? "已匹配" : "未匹配");

                    BigDecimal amount = te.getAmount();
                    if (displayCurrFinal != null && !bankAccount.getCurrency().getId().equals(displayCurrFinal.getId())) {
                        LocalDate today = LocalDate.now();
                        amount = convert(amount, bankAccount.getCurrency(), displayCurrFinal, today);
                    }
                    dto.setAmount(amount);
                    return dto;
                }).toList()
        );
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


