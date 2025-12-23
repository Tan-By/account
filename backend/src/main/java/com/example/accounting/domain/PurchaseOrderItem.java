package com.example.accounting.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 订货单明细实体
 */
@Entity
@Table(name = "purchase_order_items")
@Getter
@Setter
@NoArgsConstructor
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 所属订货单
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    /**
     * 商品（可选，如果为null则使用手动输入的商品信息）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    /**
     * 商品名称（手动输入）
     */
    @Column(nullable = false, length = 128)
    private String productName;

    /**
     * 商品单位（手动输入）
     */
    @Column(nullable = false, length = 16)
    private String unit;

    /**
     * 订购数量（整数）
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * 单价
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    /**
     * 金额（数量 × 单价）
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /**
     * 已交货数量（整数）
     */
    @Column(nullable = false)
    private Integer deliveredQuantity = 0;

    /**
     * 未交货数量（订购数量 - 已交货数量，整数）
     */
    @Column(nullable = false)
    private Integer undeliveredQuantity;
}

