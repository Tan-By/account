package com.example.accounting.repository;

import com.example.accounting.domain.PurchaseOrder;
import com.example.accounting.domain.enums.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    Optional<PurchaseOrder> findByOrderNumber(String orderNumber);
    List<PurchaseOrder> findByStatus(PurchaseOrderStatus status);
    
    @Override
    @EntityGraph(attributePaths = {"supplier", "warehouse", "submitter", "approver", "items", "items.product"})
    List<PurchaseOrder> findAll();
    
    @EntityGraph(attributePaths = {"supplier", "warehouse", "submitter", "approver", "items", "items.product"})
    @Query("SELECT p FROM PurchaseOrder p WHERE p.status = :status")
    List<PurchaseOrder> findByStatusWithGraph(PurchaseOrderStatus status);
}

