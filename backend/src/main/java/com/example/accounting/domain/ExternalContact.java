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

    @Column(length = 32, unique = true)
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


