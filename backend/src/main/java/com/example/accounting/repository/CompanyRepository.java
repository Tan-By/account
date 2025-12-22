package com.example.accounting.repository;

import com.example.accounting.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findTopByOrderByIdAsc();
}


