package com.example.accounting.repository;

import com.example.accounting.domain.ExternalContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExternalContactRepository extends JpaRepository<ExternalContact, Long> {

    Optional<ExternalContact> findByTaxIdAndStatus(String taxId, String status);
}


