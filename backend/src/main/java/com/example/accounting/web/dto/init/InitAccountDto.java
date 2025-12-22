package com.example.accounting.web.dto.init;

import com.example.accounting.domain.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InitAccountDto {

    @NotBlank
    private String name;

    private String code;

    private String parentCode;

    @NotNull
    private AccountType type;

    @NotBlank
    private String currencyCode;

    /**
     * 期初余额（以账户币种计量）
     */
    @NotNull
    private BigDecimal initialBalance;
}


