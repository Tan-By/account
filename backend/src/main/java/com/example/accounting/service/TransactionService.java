package com.example.accounting.service;

import com.example.accounting.domain.Account;
import com.example.accounting.domain.Currency;
import com.example.accounting.domain.ExchangeRate;
import com.example.accounting.domain.Transaction;
import com.example.accounting.domain.TransactionEntry;
import com.example.accounting.domain.enums.DebitCredit;
import com.example.accounting.repository.AccountRepository;
import com.example.accounting.repository.CurrencyRepository;
import com.example.accounting.repository.ExchangeRateRepository;
import com.example.accounting.repository.TransactionRepository;
import com.example.accounting.web.dto.TransactionCreateRequest;
import com.example.accounting.web.dto.TransactionDto;
import com.example.accounting.web.dto.TransactionEntryDetailDto;
import com.example.accounting.web.dto.TransactionEntryDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CurrencyRepository currencyRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountRepository accountRepository,
                              CurrencyRepository currencyRepository,
                              ExchangeRateRepository exchangeRateRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.currencyRepository = currencyRepository;
        this.exchangeRateRepository = exchangeRateRepository;
    }

    @Transactional
    public Transaction create(TransactionCreateRequest request) {
        // 借贷平衡校验
        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;
        for (TransactionEntryDto e : request.getEntries()) {
            if (e.getDebitCredit() == DebitCredit.DEBIT) {
                debitTotal = debitTotal.add(e.getAmount());
            } else {
                creditTotal = creditTotal.add(e.getAmount());
            }
        }
        if (debitTotal.compareTo(creditTotal) != 0) {
            throw new IllegalArgumentException("借贷不平衡：借方=" + debitTotal + " 贷方=" + creditTotal);
        }

        Transaction tx = new Transaction();
        tx.setDate(request.getDate());
        tx.setDescription(request.getDescription());
        tx.setStatus(com.example.accounting.domain.enums.VoucherStatus.UNAUDITED); // 保存时状态为"未审核"

        for (TransactionEntryDto dto : request.getEntries()) {
            Account account = accountRepository.findById(dto.getAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("账户不存在: " + dto.getAccountId()));
            if (!account.isEnabled()) {
                throw new IllegalArgumentException("账户已禁用: " + account.getName());
            }

            TransactionEntry entry = new TransactionEntry();
            entry.setTransaction(tx);
            entry.setAccount(account);
            entry.setDebitCredit(dto.getDebitCredit());
            entry.setAmount(dto.getAmount());
            tx.getEntries().add(entry);

            // 注意：记账时只保存凭证，不更新账户余额
            // 账户余额在过账时才更新（见PostingService）
        }

        return transactionRepository.save(tx);
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> listAll(String displayCurrency) {
        Currency displayCurr = null;
        if (displayCurrency != null && !displayCurrency.isEmpty()) {
            displayCurr = currencyRepository.findByCode(displayCurrency).orElse(null);
        }
        
        final Currency finalDisplayCurr = displayCurr;
        LocalDate today = LocalDate.now();
        
        List<Transaction> transactions = transactionRepository.findAll();
        return transactions.stream()
                .map(tx -> toDto(tx, finalDisplayCurr, today))
                .collect(Collectors.toList());
    }

    /**
     * 将Transaction实体列表转换为DTO列表
     */
    @Transactional(readOnly = true)
    public List<TransactionDto> toDtoList(List<Transaction> transactions, String displayCurrency) {
        Currency displayCurr = null;
        if (displayCurrency != null && !displayCurrency.isEmpty()) {
            displayCurr = currencyRepository.findByCode(displayCurrency).orElse(null);
        }
        
        final Currency finalDisplayCurr = displayCurr;
        LocalDate today = LocalDate.now();
        
        return transactions.stream()
                .map(tx -> toDto(tx, finalDisplayCurr, today))
                .collect(Collectors.toList());
    }

    private TransactionDto toDto(Transaction tx, Currency displayCurrency, LocalDate date) {
        TransactionDto dto = new TransactionDto();
        dto.setId(tx.getId());
        dto.setDate(tx.getDate());
        dto.setDescription(tx.getDescription());
        dto.setStatus(tx.getStatus());
        
        List<TransactionEntryDetailDto> entryDtos = tx.getEntries().stream()
                .map(entry -> {
                    TransactionEntryDetailDto entryDto = new TransactionEntryDetailDto();
                    entryDto.setId(entry.getId());
                    entryDto.setAccountId(entry.getAccount().getId());
                    entryDto.setAccountName(entry.getAccount().getName());
                    entryDto.setCurrencyCode(entry.getAccount().getCurrency().getCode());
                    entryDto.setDebitCredit(entry.getDebitCredit());
                    
                    // 如果指定了显示币种，则转换金额
                    if (displayCurrency != null && !entry.getAccount().getCurrency().getId().equals(displayCurrency.getId())) {
                        BigDecimal convertedAmount = convert(entry.getAmount(), entry.getAccount().getCurrency(), displayCurrency, date);
                        entryDto.setAmount(convertedAmount);
                    } else {
                        entryDto.setAmount(entry.getAmount());
                    }
                    
                    return entryDto;
                })
                .collect(Collectors.toList());
        
        dto.setEntries(entryDtos);
        return dto;
    }

    /**
     * 审核凭证
     */
    @Transactional
    public Transaction audit(Long transactionId) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("凭证不存在: " + transactionId));
        
        if (tx.getStatus() != com.example.accounting.domain.enums.VoucherStatus.UNAUDITED) {
            throw new IllegalArgumentException("只能审核状态为'未审核'的凭证，当前状态: " + tx.getStatus());
        }
        
        tx.setStatus(com.example.accounting.domain.enums.VoucherStatus.AUDITED);
        return transactionRepository.save(tx);
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


