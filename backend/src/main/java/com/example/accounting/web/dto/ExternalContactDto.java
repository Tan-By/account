package com.example.accounting.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExternalContactDto {

    private Long id;

    @NotBlank
    private String name;

    @NotBlank
    private String type;

    private String taxId;

    private String address;

    private String phone;

    private String email;

    private String mainContact;

    private String bankName;

    private String bankAccount;

    private String status;
}


