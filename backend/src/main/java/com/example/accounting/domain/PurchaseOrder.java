package com.example.accounting.domain;

import com.example.accounting.domain.enums.PurchaseOrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 订货单实体
 */
@Entity
@Table(name = "purchase_orders")
@Getter
@Setter
@NoArgsConstructor
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 订单编号（自动生成：PO+YYYYMMDD+流水号）
     */
    @Column(nullable = false, unique = true, length = 32)
    private String orderNumber;

    /**
     * 供货商
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private ExternalContact supplier;

    /**
     * 订单日期
     */
    @Column(nullable = false)
    private LocalDate orderDate;

    /**
     * 交货日期
     */
    @Column(nullable = false)
    private LocalDate deliveryDate;

    /**
     * 收货仓库
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    /**
     * 订单总金额
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /**
     * 状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;

    /**
     * 提交人
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitter_id")
    private UserAccount submitter;

    /**
     * 提交时间
     */
    private LocalDate submitTime;

    /**
     * 审批人
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private UserAccount approver;

    /**
     * 审批时间
     */
    private LocalDate approvalTime;

    /**
     * 审批意见
     */
    @Column(length = 512)
    private String approvalComment;

    /**
     * 订单明细
     */
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderItem> items = new ArrayList<>();
}

