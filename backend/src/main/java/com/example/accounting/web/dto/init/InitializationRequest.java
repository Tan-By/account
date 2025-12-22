package com.example.accounting.web.dto.init;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class InitializationRequest {

    @NotBlank
    private String companyName;

    private String taxId;

    private String accountingStandard;

    @NotBlank
    private String defaultCurrencyCode;

    @NotNull
    private LocalDate startDate;

    @Valid
    @Size(min = 1, message = "初始化至少需要一个会计科目")
    private List<InitAccountDto> accounts;
}


