package com.example.accounting.repository;

import com.example.accounting.domain.Account;
import com.example.accounting.domain.TransactionEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TransactionEntryRepository extends JpaRepository<TransactionEntry, Long> {

    List<TransactionEntry> findByAccountAndTransaction_DateBetween(Account account, LocalDate start, LocalDate end);
}


