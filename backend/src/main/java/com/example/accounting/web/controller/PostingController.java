package com.example.accounting.web.controller;

import com.example.accounting.service.PostingService;
import com.example.accounting.service.TransactionService;
import com.example.accounting.web.dto.PostingRequest;
import com.example.accounting.web.dto.PostingResultDto;
import com.example.accounting.web.dto.TransactionDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 过账控制器
 */
@RestController
@RequestMapping("/api/posting")
@CrossOrigin
public class PostingController {

    private final PostingService postingService;
    private final TransactionService transactionService;

    public PostingController(PostingService postingService, TransactionService transactionService) {
        this.postingService = postingService;
        this.transactionService = transactionService;
    }

    /**
     * 获取待过账凭证列表（状态为"已审核"）
     */
    @GetMapping("/pending")
    public List<TransactionDto> getPendingVouchers() {
        // 使用PostingService获取待过账凭证，然后转换为DTO
        List<com.example.accounting.domain.Transaction> vouchers = postingService.getPendingPostingVouchers();
        return transactionService.toDtoList(vouchers, null);
    }

    /**
     * 执行过账
     */
    @PostMapping("/execute")
    public ResponseEntity<PostingResultDto> executePosting(@Valid @RequestBody PostingRequest request) {
        try {
            int successCount = postingService.postVouchers(request.getVoucherIds());
            PostingResultDto result = new PostingResultDto();
            result.setSuccessCount(successCount);
            result.setMessage("成功过账 " + successCount + " 张凭证");
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            PostingResultDto result = new PostingResultDto();
            result.setSuccessCount(0);
            result.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
}

