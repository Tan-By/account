package com.example.accounting.web.dto;

import com.example.accounting.domain.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountDto {

    private Long id;

    @NotBlank
    private String name;

    private String code;

    @NotNull
    private AccountType type;

    private Long parentId;

    /**
     * 币种代码，如 CNY、USD
     */
    @NotBlank
    private String currencyCode;

    private BigDecimal balance;

    private boolean enabled = true;
}


