package com.example.accounting.web.controller;

import com.example.accounting.service.PurchaseOrderService;
import com.example.accounting.web.dto.purchase.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/purchase-orders")
@CrossOrigin
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    /**
     * 查询所有订货单
     */
    @GetMapping
    public List<PurchaseOrderDto> list() {
        return purchaseOrderService.listAll();
    }

    /**
     * 查询单个订货单
     */
    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrderDto> get(@PathVariable Long id) {
        try {
            PurchaseOrderDto dto = purchaseOrderService.findById(id);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 创建订货单
     */
    @PostMapping
    public ResponseEntity<PurchaseOrderDto> create(@Valid @RequestBody PurchaseOrderCreateRequest request,
                                                    @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        // TODO: 从JWT token或session中获取当前用户ID
        if (userId == null) {
            userId = 1L; // 临时默认值
        }
        try {
            PurchaseOrderDto dto = purchaseOrderService.create(request, userId);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 提交审批
     */
    @PostMapping("/{id}/submit")
    public ResponseEntity<PurchaseOrderDto> submit(@PathVariable Long id,
                                                    @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            userId = 1L;
        }
        try {
            PurchaseOrderDto dto = purchaseOrderService.submitForApproval(id, userId);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 审批订货单
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<PurchaseOrderDto> approve(@PathVariable Long id,
                                                     @Valid @RequestBody PurchaseOrderApproveRequest request,
                                                     @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            userId = 1L;
        }
        try {
            PurchaseOrderDto dto = purchaseOrderService.approve(id, request, userId);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 收货操作
     */
    @PostMapping("/{id}/deliver")
    public ResponseEntity<PurchaseOrderDto> deliver(@PathVariable Long id,
                                                     @Valid @RequestBody List<DeliveryRequest> deliveries) {
        try {
            PurchaseOrderDto dto = purchaseOrderService.deliver(id, deliveries);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

