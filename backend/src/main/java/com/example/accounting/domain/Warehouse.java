package com.example.accounting.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 仓库实体
 */
@Entity
@Table(name = "warehouses")
@Getter
@Setter
@NoArgsConstructor
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 仓库编码
     */
    @Column(nullable = false, unique = true, length = 32)
    private String code;

    /**
     * 仓库名称
     */
    @Column(nullable = false, length = 128)
    private String name;

    /**
     * 仓库地址
     */
    @Column(length = 256)
    private String address;

    /**
     * 是否启用
     */
    @Column(nullable = false)
    private boolean enabled = true;
}

