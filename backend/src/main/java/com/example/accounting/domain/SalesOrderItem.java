package com.example.accounting.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 销售单明细实体
 */
@Entity
@Table(name = "sales_order_items")
@Getter
@Setter
@NoArgsConstructor
public class SalesOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 所属销售单
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_id", nullable = false)
    private SalesOrder salesOrder;

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
     * 销售数量（整数）
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * 销售单价
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    /**
     * 折扣率（0-1之间的小数，如0.1表示10%折扣）
     */
    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal discountRate = BigDecimal.ZERO;

    /**
     * 金额（数量 × 单价 × (1 - 折扣率)）
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /**
     * 已发货数量（整数）
     */
    @Column(nullable = false)
    private Integer shippedQuantity = 0;

    /**
     * 未发货数量（销售数量 - 已发货数量，整数）
     */
    @Column(nullable = false)
    private Integer unshippedQuantity;
}

