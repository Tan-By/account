package com.example.accounting.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 交易日期
     */
    @Column(nullable = false)
    private LocalDate date;

    /**
     * 描述/摘要
     */
    @Column(nullable = false, length = 256)
    private String description;

    /**
     * 分录集合（借方/贷方行）
     */
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransactionEntry> entries = new ArrayList<>();
}


