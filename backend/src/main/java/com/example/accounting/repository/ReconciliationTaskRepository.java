package com.example.accounting.repository;

import com.example.accounting.domain.ReconciliationTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconciliationTaskRepository extends JpaRepository<ReconciliationTask, Long> {
}


