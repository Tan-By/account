package com.example.accounting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "currencies")
@Getter
@Setter
@NoArgsConstructor
public class Currency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 货币代码，如 CNY、USD、EUR
     */
    @Column(nullable = false, unique = true, length = 8)
    private String code;

    /**
     * 货币名称，如 人民币、美元
     */
    @Column(nullable = false, length = 64)
    private String name;

    /**
     * 小数位数，例如：人民币2位
     */
    @Column(nullable = false)
    private int fractionDigits = 2;
}


