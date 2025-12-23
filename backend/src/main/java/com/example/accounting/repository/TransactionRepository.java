package com.example.accounting.repository;

import com.example.accounting.domain.Transaction;
import com.example.accounting.domain.enums.VoucherStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    /**
     * 根据凭证状态查询
     */
    @EntityGraph(attributePaths = {"entries", "entries.account", "entries.account.currency"})
    @Query("SELECT t FROM Transaction t WHERE t.status = :status")
    List<Transaction> findByStatus(VoucherStatus status);

    /**
     * 根据日期范围查询交易
     */
    @EntityGraph(attributePaths = {"entries", "entries.account", "entries.account.currency"})
    @Query("SELECT t FROM Transaction t WHERE t.date BETWEEN :startDate AND :endDate")
    List<Transaction> findByDateBetween(LocalDate startDate, LocalDate endDate);
    
    @Override
    @EntityGraph(attributePaths = {"entries", "entries.account", "entries.account.currency"})
    List<Transaction> findAll();
    
    /**
     * 根据ID列表查询，预加载关联对象
     */
    @EntityGraph(attributePaths = {"entries", "entries.account", "entries.account.currency"})
    @Query("SELECT t FROM Transaction t WHERE t.id IN :ids")
    List<Transaction> findAllByIdWithGraph(List<Long> ids);
}


