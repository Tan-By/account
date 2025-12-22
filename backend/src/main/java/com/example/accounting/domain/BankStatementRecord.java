package com.example.accounting.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "bank_statement_records")
@Getter
@Setter
@NoArgsConstructor
public class BankStatementRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account bankAccount; // 对应企业账面中的银行账户

    @Column(nullable = false)
    private LocalDate date;

    @Column(length = 256)
    private String description;

    @Column(precision = 19, scale = 4)
    private BigDecimal debitAmount;

    @Column(precision = 19, scale = 4)
    private BigDecimal creditAmount;

    @Column(precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(length = 64)
    private String reference; // 流水号

    @Column(length = 32)
    private String matchStatus; // 已匹配 | 未匹配 | 待确认
}


