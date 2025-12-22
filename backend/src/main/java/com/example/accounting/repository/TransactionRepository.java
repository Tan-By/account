package com.example.accounting.repository;

import com.example.accounting.domain.Transaction;
import com.example.accounting.domain.enums.VoucherStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    /**
     * 根据凭证状态查询
     */
    List<Transaction> findByStatus(VoucherStatus status);

    /**
     * 根据日期范围查询交易
     */
    List<Transaction> findByDateBetween(LocalDate startDate, LocalDate endDate);
}


