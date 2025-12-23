package com.example.accounting.repository;

import com.example.accounting.domain.SalesOrder;
import com.example.accounting.domain.enums.SalesOrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {
    Optional<SalesOrder> findByOrderNumber(String orderNumber);
    List<SalesOrder> findByStatus(SalesOrderStatus status);
    
    @Override
    @EntityGraph(attributePaths = {"customer", "submitter", "auditor", "items", "items.product"})
    List<SalesOrder> findAll();
    
    @EntityGraph(attributePaths = {"customer", "submitter", "auditor", "items", "items.product"})
    @Query("SELECT s FROM SalesOrder s WHERE s.status = :status")
    List<SalesOrder> findByStatusWithGraph(SalesOrderStatus status);
}

