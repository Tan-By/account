package com.example.accounting.repository;

import com.example.accounting.domain.Account;
import com.example.accounting.domain.BankStatementRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BankStatementRecordRepository extends JpaRepository<BankStatementRecord, Long> {

    List<BankStatementRecord> findByBankAccountAndDateBetween(Account bankAccount, LocalDate start, LocalDate end);
}


