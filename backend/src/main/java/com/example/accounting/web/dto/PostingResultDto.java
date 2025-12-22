package com.example.accounting.web.dto;

import lombok.Data;

/**
 * 过账结果DTO
 */
@Data
public class PostingResultDto {
    /**
     * 成功过账的凭证数量
     */
    private int successCount;
    
    /**
     * 消息
     */
    private String message;
}


