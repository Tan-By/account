package com.example.accounting.repository;

import com.example.accounting.domain.TaxDeclaration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxDeclarationRepository extends JpaRepository<TaxDeclaration, Long> {
}


