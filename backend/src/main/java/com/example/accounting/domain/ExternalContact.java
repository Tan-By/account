package com.example.accounting.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "external_contacts")
@Getter
@Setter
@NoArgsConstructor
public class ExternalContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 32)
    private String type; // 供应商 | 客户 | ...

    /**
     * 纳税人识别号
     * 业务规则：在“活跃”状态下唯一，因此这里不加数据库唯一约束，
     * 由业务代码通过 findByTaxIdAndStatus('活跃') 保证逻辑唯一性，
     * 这样在停用旧联系人后可以创建同税号的新联系人。
     */
    @Column(length = 32)
    private String taxId;

    @Column(length = 256)
    private String address;

    @Column(length = 32)
    private String phone;

    @Column(length = 64)
    private String email;

    @Column(length = 64)
    private String mainContact;

    @Column(length = 64)
    private String bankName;

    @Column(length = 64)
    private String bankAccount;

    @Column(nullable = false, length = 16)
    private String status = "活跃"; // 活跃 | 停用
}


