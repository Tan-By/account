package com.example.accounting.web.controller;

import com.example.accounting.service.SalesOrderService;
import com.example.accounting.web.dto.sales.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sales-orders")
@CrossOrigin
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    public SalesOrderController(SalesOrderService salesOrderService) {
        this.salesOrderService = salesOrderService;
    }

    /**
     * 查询所有销售单
     */
    @GetMapping
    public List<SalesOrderDto> list() {
        return salesOrderService.listAll();
    }

    /**
     * 查询单个销售单
     */
    @GetMapping("/{id}")
    public ResponseEntity<SalesOrderDto> get(@PathVariable Long id) {
        try {
            SalesOrderDto dto = salesOrderService.findById(id);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 创建销售单
     */
    @PostMapping
    public ResponseEntity<SalesOrderDto> create(@Valid @RequestBody SalesOrderCreateRequest request,
                                                 @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        // TODO: 从JWT token或session中获取当前用户ID
        if (userId == null) {
            userId = 1L; // 临时默认值
        }
        try {
            SalesOrderDto dto = salesOrderService.create(request, userId);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 提交审核
     */
    @PostMapping("/{id}/submit")
    public ResponseEntity<SalesOrderDto> submit(@PathVariable Long id,
                                                  @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            userId = 1L;
        }
        try {
            SalesOrderDto dto = salesOrderService.submitForAudit(id, userId);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 审核销售单
     */
    @PostMapping("/{id}/audit")
    public ResponseEntity<SalesOrderDto> audit(@PathVariable Long id,
                                                @Valid @RequestBody SalesOrderAuditRequest request,
                                                @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            userId = 1L;
        }
        try {
            SalesOrderDto dto = salesOrderService.audit(id, request, userId);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 发货操作
     */
    @PostMapping("/{id}/ship")
    public ResponseEntity<SalesOrderDto> ship(@PathVariable Long id,
                                                @Valid @RequestBody List<ShipmentRequest> shipments) {
        try {
            SalesOrderDto dto = salesOrderService.ship(id, shipments);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 收款操作
     */
    @PostMapping("/{id}/payment")
    public ResponseEntity<SalesOrderDto> receivePayment(@PathVariable Long id,
                                                          @Valid @RequestBody PaymentRequest request) {
        try {
            SalesOrderDto dto = salesOrderService.receivePayment(id, request);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

