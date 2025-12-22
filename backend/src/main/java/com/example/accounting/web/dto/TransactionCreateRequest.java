package com.example.accounting.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class TransactionCreateRequest {

    @NotNull
    private LocalDate date;

    @NotBlank
    private String description;

    @Valid
    @Size(min = 2, message = "一笔交易至少需要两条分录")
    private List<TransactionEntryDto> entries;
}


