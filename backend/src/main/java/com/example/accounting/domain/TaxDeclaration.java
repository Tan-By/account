package com.example.accounting.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tax_declarations")
@Getter
@Setter
@NoArgsConstructor
public class TaxDeclaration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String taxType; // 增值税、企业所得税等

    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    @Column(nullable = false, length = 32)
    private String status; // 草稿 | 已提交 | 已受理 | 已退回 | 申报成功

    @Column(precision = 19, scale = 2)
    private BigDecimal taxableAmount; // 计税依据

    @Column(precision = 19, scale = 2)
    private BigDecimal taxPayable; // 应纳税额

    @Column(precision = 19, scale = 2)
    private BigDecimal taxRelief; // 减免税额

    @Column(precision = 19, scale = 2)
    private BigDecimal taxPaid; // 已缴税额

    @Column(precision = 19, scale = 2)
    private BigDecimal taxDue; // 应补(退)税额

    private LocalDateTime submittedAt;

    @Column(length = 256)
    private String remark;
}


