package com.example.accounting.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 商品实体
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 商品编码
     */
    @Column(nullable = false, unique = true, length = 64)
    private String code;

    /**
     * 商品名称
     */
    @Column(nullable = false, length = 128)
    private String name;

    /**
     * 规格型号
     */
    @Column(length = 128)
    private String specification;

    /**
     * 单位（如：个、箱、公斤等）
     */
    @Column(nullable = false, length = 16)
    private String unit;

    /**
     * 最近采购价（用于参考）
     */
    @Column(precision = 19, scale = 2)
    private BigDecimal lastPurchasePrice;

    /**
     * 是否启用
     */
    @Column(nullable = false)
    private boolean enabled = true;
}

