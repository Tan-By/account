package com.example.accounting.repository;

import com.example.accounting.domain.Currency;
import com.example.accounting.domain.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    Optional<ExchangeRate> findTopByFromCurrencyAndToCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(
            Currency fromCurrency, Currency toCurrency, LocalDate rateDate);

    @Query("SELECT er FROM ExchangeRate er JOIN FETCH er.fromCurrency JOIN FETCH er.toCurrency")
    List<ExchangeRate> findAllWithCurrencies();

    @Query("SELECT COUNT(er) FROM ExchangeRate er " +
           "JOIN er.fromCurrency fc JOIN er.toCurrency tc " +
           "WHERE fc.code = :fromCode AND tc.code = :toCode AND er.rateDate = :rateDate")
    long countByCurrencyCodesAndDate(@Param("fromCode") String fromCode, 
                                     @Param("toCode") String toCode, 
                                     @Param("rateDate") LocalDate rateDate);
}


