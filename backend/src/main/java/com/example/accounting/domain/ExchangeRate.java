package com.example.accounting.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "exchange_rates")
@Getter
@Setter
@NoArgsConstructor
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_currency_id", nullable = false)
    private Currency fromCurrency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_currency_id", nullable = false)
    private Currency toCurrency;

    /**
     * 汇率：1 fromCurrency = rate toCurrency
     */
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal rate;

    /**
     * 汇率日期
     */
    @Column(nullable = false)
    private LocalDate rateDate;
}


