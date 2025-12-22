package com.example.accounting.web.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Set;

@Data
public class UserDto {

    private Long id;

    @NotBlank
    private String name;

    @NotBlank
    private String username;

    private String employeeNo;

    private String department;

    private String position;

    private String status;

    private Set<String> roles;

    private String password; // 仅用于创建或重置
}


