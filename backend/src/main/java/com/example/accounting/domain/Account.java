package com.example.accounting.domain;

import com.example.accounting.domain.enums.AccountType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 账户名称，例如：银行存款、固定资产:电脑
     */
    @Column(nullable = false, length = 128)
    private String name;

    /**
     * 账户编码，可选，用于排序和对齐会计制度
     */
    @Column(length = 32)
    private String code;

    /**
     * 账户类型：资产、负债、权益、收入、支出
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AccountType type;

    /**
     * 上级账户（支持树状结构）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Account parent;

    /**
     * 币种，多币种支持的关键字段
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id", nullable = false)
    private Currency currency;

    /**
     * 当前余额（以账户本币计量）
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    /**
     * 是否启用
     */
    @Column(nullable = false)
    private boolean enabled = true;
}


