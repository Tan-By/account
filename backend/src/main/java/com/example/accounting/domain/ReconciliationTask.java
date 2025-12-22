package com.example.accounting.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reconciliation_tasks")
@Getter
@Setter
@NoArgsConstructor
public class ReconciliationTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account bankAccount;

    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    @Column(length = 32, nullable = false)
    private String status; // 进行中 | 已完成

    @Column(length = 64)
    private String executor;

    private LocalDateTime executedAt;
}


