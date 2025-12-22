package com.example.accounting.web.dto;

import com.example.accounting.domain.enums.VoucherStatus;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class TransactionDto {
    private Long id;
    private LocalDate date;
    private String description;
    private VoucherStatus status;
    private List<TransactionEntryDetailDto> entries;
}

