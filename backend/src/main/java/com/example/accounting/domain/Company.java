package com.example.accounting.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 64)
    private String taxId;

    @Column(length = 64)
    private String accountingStandard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_currency_id")
    private Currency defaultCurrency;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private boolean initialized = false;
}


