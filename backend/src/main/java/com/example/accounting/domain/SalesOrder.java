package com.example.accounting.domain;

import com.example.accounting.domain.enums.SalesOrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 销售单实体
 */
@Entity
@Table(name = "sales_orders")
@Getter
@Setter
@NoArgsConstructor
public class SalesOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 销售单号（自动生成：SO+YYYYMMDD+流水号）
     */
    @Column(nullable = false, unique = true, length = 32)
    private String orderNumber;

    /**
     * 客户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private ExternalContact customer;

    /**
     * 下单日期
     */
    @Column(nullable = false)
    private LocalDate orderDate;

    /**
     * 交货日期
     */
    @Column(nullable = false)
    private LocalDate deliveryDate;

    /**
     * 发货地址
     */
    @Column(length = 256)
    private String shippingAddress;

    /**
     * 收款方式
     */
    @Column(length = 32)
    private String paymentMethod;

    /**
     * 订单总金额
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /**
     * 折扣总额
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /**
     * 应收总额（订单总金额 - 折扣总额）
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal receivableAmount = BigDecimal.ZERO;

    /**
     * 已收金额
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal receivedAmount = BigDecimal.ZERO;

    /**
     * 未收金额（应收总额 - 已收金额）
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unreceivedAmount = BigDecimal.ZERO;

    /**
     * 开票状态
     */
    @Column(length = 16)
    private String invoiceStatus = "未开票"; // 未开票|已开票|部分开票

    /**
     * 状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SalesOrderStatus status = SalesOrderStatus.DRAFT;

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
     * 审核人
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auditor_id")
    private UserAccount auditor;

    /**
     * 审核时间
     */
    private LocalDate auditTime;

    /**
     * 审核意见
     */
    @Column(length = 512)
    private String auditComment;

    /**
     * 订单明细
     */
    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SalesOrderItem> items = new ArrayList<>();
}

