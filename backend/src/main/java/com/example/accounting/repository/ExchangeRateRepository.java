package com.example.accounting.repository;

import com.example.accounting.domain.Currency;
import com.example.accounting.domain.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    Optional<ExchangeRate> findTopByFromCurrencyAndToCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(
            Currency fromCurrency, Currency toCurrency, LocalDate rateDate);
}


