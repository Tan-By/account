package com.example.accounting.web.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 过账请求DTO
 */
@Data
public class PostingRequest {
    /**
     * 要过账的凭证ID列表
     */
    @NotEmpty(message = "请选择要过账的凭证")
    private List<Long> voucherIds;
}







