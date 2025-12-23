package com.example.accounting.service;

import com.example.accounting.domain.*;
import com.example.accounting.domain.enums.AccountType;
import com.example.accounting.domain.enums.DebitCredit;
import com.example.accounting.domain.enums.PurchaseOrderStatus;
import com.example.accounting.domain.enums.VoucherStatus;
import com.example.accounting.repository.*;
import com.example.accounting.web.dto.purchase.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ExternalContactRepository externalContactRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserAccountRepository userAccountRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public PurchaseOrderService(
            PurchaseOrderRepository purchaseOrderRepository,
            ExternalContactRepository externalContactRepository,
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository,
            UserAccountRepository userAccountRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.externalContactRepository = externalContactRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.userAccountRepository = userAccountRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * 生成订单编号：PO+YYYYMMDD+流水号
     */
    private String generateOrderNumber() {
        String prefix = "PO" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        List<PurchaseOrder> todayOrders = purchaseOrderRepository.findAll().stream()
                .filter(po -> po.getOrderNumber().startsWith(prefix))
                .collect(Collectors.toList());
        int sequence = todayOrders.size() + 1;
        return String.format("%s%03d", prefix, sequence);
    }

    /**
     * 创建订货单
     */
    @Transactional
    public PurchaseOrderDto create(PurchaseOrderCreateRequest request, Long userId) {
        // 验证供货商
        ExternalContact supplier = externalContactRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new IllegalArgumentException("供货商不存在"));
        if (!"活跃".equals(supplier.getStatus())) {
            throw new IllegalArgumentException("该供货商已被禁用，请选择其他供货商");
        }
        if (!"供应商".equals(supplier.getType())) {
            throw new IllegalArgumentException("选择的联系人不是供应商");
        }

        // 验证仓库（如果提供）
        Warehouse warehouse = null;
        if (request.getWarehouseId() != null) {
            warehouse = warehouseRepository.findById(request.getWarehouseId())
                    .orElseThrow(() -> new IllegalArgumentException("仓库不存在"));
            if (!warehouse.isEnabled()) {
                throw new IllegalArgumentException("仓库已被禁用");
            }
        }

        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNumber(generateOrderNumber());
        order.setSupplier(supplier);
        order.setOrderDate(request.getOrderDate());
        order.setDeliveryDate(request.getDeliveryDate());
        order.setWarehouse(warehouse);
        order.setStatus(PurchaseOrderStatus.DRAFT);
        order.setSubmitter(userAccountRepository.findById(userId).orElse(null));

        BigDecimal totalAmount = BigDecimal.ZERO;

        // 处理订单明细
        for (PurchaseOrderItemCreateRequest itemReq : request.getItems()) {
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setPurchaseOrder(order);
            
            // 如果提供了productId，则使用商品信息，否则使用手动输入的信息
            if (itemReq.getProductId() != null) {
                Product product = productRepository.findById(itemReq.getProductId())
                        .orElseThrow(() -> new IllegalArgumentException("商品不存在: " + itemReq.getProductId()));
                if (!product.isEnabled()) {
                    throw new IllegalArgumentException("商品已停用: " + product.getName());
                }
                item.setProduct(product);
                item.setProductName(product.getName());
                item.setUnit(product.getUnit());
                
                // 更新商品的最近采购价
                product.setLastPurchasePrice(itemReq.getUnitPrice());
                productRepository.save(product);
            } else {
                // 使用手动输入的商品信息
                item.setProduct(null);
                item.setProductName(itemReq.getProductName());
                item.setUnit(itemReq.getUnit());
            }
            
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(itemReq.getUnitPrice());
            BigDecimal amount = BigDecimal.valueOf(itemReq.getQuantity()).multiply(itemReq.getUnitPrice());
            item.setAmount(amount);
            item.setDeliveredQuantity(0);
            item.setUndeliveredQuantity(itemReq.getQuantity());

            order.getItems().add(item);
            totalAmount = totalAmount.add(amount);
        }

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("订单总金额必须大于零");
        }

        order.setTotalAmount(totalAmount);
        PurchaseOrder saved = purchaseOrderRepository.save(order);
        return toDto(saved);
    }

    /**
     * 提交审批
     */
    @Transactional
    public PurchaseOrderDto submitForApproval(Long orderId, Long userId) {
        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订货单不存在"));
        
        if (order.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new IllegalArgumentException("只能提交草稿状态的订货单");
        }

        order.setStatus(PurchaseOrderStatus.PENDING_APPROVAL);
        order.setSubmitter(userAccountRepository.findById(userId).orElse(null));
        order.setSubmitTime(LocalDate.now());

        PurchaseOrder saved = purchaseOrderRepository.save(order);
        return toDto(saved);
    }

    /**
     * 审批订货单
     */
    @Transactional
    public PurchaseOrderDto approve(Long orderId, PurchaseOrderApproveRequest request, Long userId) {
        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订货单不存在"));
        
        if (order.getStatus() != PurchaseOrderStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("只能审批待审批状态的订货单");
        }

        UserAccount approver = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        if (request.isApproved()) {
            order.setStatus(PurchaseOrderStatus.APPROVED);
            order.setApprover(approver);
            order.setApprovalTime(LocalDate.now());
            order.setApprovalComment(request.getComment());

            // 生成应付账款凭证
            generateAccountsPayableVoucher(order);
        } else {
            order.setStatus(PurchaseOrderStatus.REJECTED);
            order.setApprover(approver);
            order.setApprovalTime(LocalDate.now());
            order.setApprovalComment(request.getComment());
        }

        PurchaseOrder saved = purchaseOrderRepository.save(order);
        return toDto(saved);
    }

    /**
     * 生成应付账款凭证
     */
    private void generateAccountsPayableVoucher(PurchaseOrder order) {
        // 查找或创建应付账款科目（负债类）
        Account accountsPayable = accountRepository.findAll().stream()
                .filter(a -> a.getName().contains("应付账款") && a.getType() == AccountType.LIABILITY)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("系统中不存在'应付账款'科目，请先创建"));

        // 查找或创建库存商品科目（资产类）
        Account inventory = accountRepository.findAll().stream()
                .filter(a -> (a.getName().contains("库存商品") || a.getName().contains("原材料")) && a.getType() == AccountType.ASSET)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("系统中不存在'库存商品'或'原材料'科目，请先创建"));

        Transaction voucher = new Transaction();
        voucher.setDate(LocalDate.now());
        voucher.setDescription("订货单审批通过 - " + order.getOrderNumber());
        voucher.setStatus(VoucherStatus.UNAUDITED); // 需要审核后才能过账

        // 借方：库存商品/原材料
        TransactionEntry debitEntry = new TransactionEntry();
        debitEntry.setTransaction(voucher);
        debitEntry.setAccount(inventory);
        debitEntry.setDebitCredit(DebitCredit.DEBIT);
        debitEntry.setAmount(order.getTotalAmount());
        voucher.getEntries().add(debitEntry);

        // 贷方：应付账款
        TransactionEntry creditEntry = new TransactionEntry();
        creditEntry.setTransaction(voucher);
        creditEntry.setAccount(accountsPayable);
        creditEntry.setDebitCredit(DebitCredit.CREDIT);
        creditEntry.setAmount(order.getTotalAmount());
        voucher.getEntries().add(creditEntry);

        transactionRepository.save(voucher);
    }

    /**
     * 收货操作
     */
    @Transactional
    public PurchaseOrderDto deliver(Long orderId, List<DeliveryRequest> deliveries) {
        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订货单不存在"));
        
        if (order.getStatus() != PurchaseOrderStatus.APPROVED && 
            order.getStatus() != PurchaseOrderStatus.PARTIAL_DELIVERY) {
            throw new IllegalArgumentException("只能对已批准或部分交货状态的订货单进行收货操作");
        }

        for (DeliveryRequest delivery : deliveries) {
            PurchaseOrderItem item = order.getItems().stream()
                    .filter(i -> i.getId().equals(delivery.getItemId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("订单明细不存在"));

            int deliveryQty = delivery.getQuantity().intValue();
            int newDelivered = item.getDeliveredQuantity() + deliveryQty;
            if (newDelivered > item.getQuantity()) {
                throw new IllegalArgumentException("已交货数量不能超过订购数量");
            }

            item.setDeliveredQuantity(newDelivered);
            item.setUndeliveredQuantity(item.getQuantity() - newDelivered);
        }

        // 更新订单状态
        boolean allDelivered = order.getItems().stream()
                .allMatch(item -> item.getDeliveredQuantity().equals(item.getQuantity()));
        
        if (allDelivered) {
            order.setStatus(PurchaseOrderStatus.COMPLETED);
        } else {
            order.setStatus(PurchaseOrderStatus.PARTIAL_DELIVERY);
        }

        PurchaseOrder saved = purchaseOrderRepository.save(order);
        return toDto(saved);
    }

    /**
     * 查询所有订货单
     */
    @Transactional(readOnly = true)
    public List<PurchaseOrderDto> listAll() {
        return purchaseOrderRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 查询单个订货单
     */
    @Transactional(readOnly = true)
    public PurchaseOrderDto findById(Long id) {
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("订货单不存在"));
        return toDto(order);
    }

    /**
     * 转换为DTO
     */
    private PurchaseOrderDto toDto(PurchaseOrder order) {
        PurchaseOrderDto dto = new PurchaseOrderDto();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setSupplierId(order.getSupplier().getId());
        dto.setSupplierName(order.getSupplier().getName());
        dto.setOrderDate(order.getOrderDate());
        dto.setDeliveryDate(order.getDeliveryDate());
        if (order.getWarehouse() != null) {
            dto.setWarehouseId(order.getWarehouse().getId());
            dto.setWarehouseName(order.getWarehouse().getName());
        }
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus());
        if (order.getSubmitter() != null) {
            dto.setSubmitterId(order.getSubmitter().getId());
            dto.setSubmitterName(order.getSubmitter().getUsername());
        }
        dto.setSubmitTime(order.getSubmitTime());
        if (order.getApprover() != null) {
            dto.setApproverId(order.getApprover().getId());
            dto.setApproverName(order.getApprover().getUsername());
        }
        dto.setApprovalTime(order.getApprovalTime());
        dto.setApprovalComment(order.getApprovalComment());
        
        dto.setItems(order.getItems().stream().map(item -> {
            PurchaseOrderItemDto itemDto = new PurchaseOrderItemDto();
            itemDto.setId(item.getId());
            if (item.getProduct() != null) {
                itemDto.setProductId(item.getProduct().getId());
                itemDto.setProductCode(item.getProduct().getCode());
                itemDto.setSpecification(item.getProduct().getSpecification());
            } else {
                itemDto.setProductId(null);
                itemDto.setProductCode(null);
                itemDto.setSpecification(null);
            }
            itemDto.setProductName(item.getProductName());
            itemDto.setUnit(item.getUnit());
            itemDto.setQuantity(BigDecimal.valueOf(item.getQuantity()));
            itemDto.setUnitPrice(item.getUnitPrice());
            itemDto.setAmount(item.getAmount());
            itemDto.setDeliveredQuantity(BigDecimal.valueOf(item.getDeliveredQuantity()));
            itemDto.setUndeliveredQuantity(BigDecimal.valueOf(item.getUndeliveredQuantity()));
            return itemDto;
        }).collect(Collectors.toList()));
        
        return dto;
    }
}

