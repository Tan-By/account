package com.example.accounting.service;

import com.example.accounting.domain.Account;
import com.example.accounting.domain.Transaction;
import com.example.accounting.domain.TransactionEntry;
import com.example.accounting.domain.enums.DebitCredit;
import com.example.accounting.repository.AccountRepository;
import com.example.accounting.repository.TransactionRepository;
import com.example.accounting.web.dto.TransactionCreateRequest;
import com.example.accounting.web.dto.TransactionEntryDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
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

            // 更新账户余额（简化版，多币种在账户层面各自维护本币余额）
            BigDecimal bal = account.getBalance();
            if (dto.getDebitCredit() == DebitCredit.DEBIT) {
                account.setBalance(bal.add(dto.getAmount()));
            } else {
                account.setBalance(bal.subtract(dto.getAmount()));
            }
            accountRepository.save(account);
        }

        return transactionRepository.save(tx);
    }
}


