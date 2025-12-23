package com.example.accounting.service;

import com.example.accounting.domain.*;
import com.example.accounting.domain.enums.AccountType;
import com.example.accounting.domain.enums.DebitCredit;
import com.example.accounting.domain.enums.SalesOrderStatus;
import com.example.accounting.domain.enums.VoucherStatus;
import com.example.accounting.repository.*;
import com.example.accounting.web.dto.sales.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final ExternalContactRepository externalContactRepository;
    private final ProductRepository productRepository;
    private final UserAccountRepository userAccountRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public SalesOrderService(
            SalesOrderRepository salesOrderRepository,
            ExternalContactRepository externalContactRepository,
            ProductRepository productRepository,
            UserAccountRepository userAccountRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository) {
        this.salesOrderRepository = salesOrderRepository;
        this.externalContactRepository = externalContactRepository;
        this.productRepository = productRepository;
        this.userAccountRepository = userAccountRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * 生成订单编号：SO+YYYYMMDD+流水号
     */
    private String generateOrderNumber() {
        String prefix = "SO" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        List<SalesOrder> todayOrders = salesOrderRepository.findAll().stream()
                .filter(so -> so.getOrderNumber().startsWith(prefix))
                .collect(Collectors.toList());
        int sequence = todayOrders.size() + 1;
        return String.format("%s%03d", prefix, sequence);
    }

    /**
     * 创建销售单
     */
    @Transactional
    public SalesOrderDto create(SalesOrderCreateRequest request, Long userId) {
        // 验证客户
        ExternalContact customer = externalContactRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("客户不存在"));
        if (!"活跃".equals(customer.getStatus())) {
            throw new IllegalArgumentException("该客户已被禁用，请选择其他客户");
        }
        if (!"客户".equals(customer.getType())) {
            throw new IllegalArgumentException("选择的联系人不是客户");
        }

        SalesOrder order = new SalesOrder();
        order.setOrderNumber(generateOrderNumber());
        order.setCustomer(customer);
        order.setOrderDate(request.getOrderDate());
        order.setDeliveryDate(request.getDeliveryDate());
        order.setShippingAddress(request.getShippingAddress());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setStatus(SalesOrderStatus.DRAFT);
        order.setSubmitter(userAccountRepository.findById(userId).orElse(null));
        order.setInvoiceStatus("未开票");

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;

        // 处理订单明细
        for (SalesOrderItemCreateRequest itemReq : request.getItems()) {
            SalesOrderItem item = new SalesOrderItem();
            item.setSalesOrder(order);
            
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
            } else {
                // 使用手动输入的商品信息
                item.setProduct(null);
                item.setProductName(itemReq.getProductName());
                item.setUnit(itemReq.getUnit());
            }
            
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(itemReq.getUnitPrice());
            
            BigDecimal discountRate = itemReq.getDiscountRate() != null ? 
                    itemReq.getDiscountRate() : BigDecimal.ZERO;
            if (discountRate.compareTo(BigDecimal.ZERO) < 0 || discountRate.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException("折扣率必须在0-1之间");
            }
            item.setDiscountRate(discountRate);
            
            BigDecimal itemAmount = BigDecimal.valueOf(itemReq.getQuantity())
                    .multiply(itemReq.getUnitPrice())
                    .multiply(BigDecimal.ONE.subtract(discountRate));
            item.setAmount(itemAmount);
            item.setShippedQuantity(0);
            item.setUnshippedQuantity(itemReq.getQuantity());

            order.getItems().add(item);
            totalAmount = totalAmount.add(BigDecimal.valueOf(itemReq.getQuantity()).multiply(itemReq.getUnitPrice()));
            totalDiscount = totalDiscount.add(BigDecimal.valueOf(itemReq.getQuantity())
                    .multiply(itemReq.getUnitPrice())
                    .multiply(discountRate));
        }

        BigDecimal receivableAmount = totalAmount.subtract(totalDiscount);
        if (receivableAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("应收总额必须大于零");
        }

        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(totalDiscount);
        order.setReceivableAmount(receivableAmount);
        order.setReceivedAmount(BigDecimal.ZERO);
        order.setUnreceivedAmount(receivableAmount);

        SalesOrder saved = salesOrderRepository.save(order);
        return toDto(saved);
    }

    /**
     * 提交审核
     */
    @Transactional
    public SalesOrderDto submitForAudit(Long orderId, Long userId) {
        SalesOrder order = salesOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("销售单不存在"));
        
        if (order.getStatus() != SalesOrderStatus.DRAFT) {
            throw new IllegalArgumentException("只能提交草稿状态的销售单");
        }

        order.setStatus(SalesOrderStatus.PENDING_AUDIT);
        order.setSubmitter(userAccountRepository.findById(userId).orElse(null));
        order.setSubmitTime(LocalDate.now());

        SalesOrder saved = salesOrderRepository.save(order);
        return toDto(saved);
    }

    /**
     * 审核销售单
     */
    @Transactional
    public SalesOrderDto audit(Long orderId, SalesOrderAuditRequest request, Long userId) {
        SalesOrder order = salesOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("销售单不存在"));
        
        if (order.getStatus() != SalesOrderStatus.PENDING_AUDIT) {
            throw new IllegalArgumentException("只能审核待审核状态的销售单");
        }

        UserAccount auditor = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        if (request.isApproved()) {
            order.setStatus(SalesOrderStatus.AUDITED);
            order.setAuditor(auditor);
            order.setAuditTime(LocalDate.now());
            order.setAuditComment(request.getComment());

            // 生成销售收入凭证和成本结转凭证
            generateSalesRevenueVoucher(order);
        } else {
            order.setStatus(SalesOrderStatus.REJECTED);
            order.setAuditor(auditor);
            order.setAuditTime(LocalDate.now());
            order.setAuditComment(request.getComment());
        }

        SalesOrder saved = salesOrderRepository.save(order);
        return toDto(saved);
    }

    /**
     * 生成销售收入凭证和成本结转凭证
     */
    private void generateSalesRevenueVoucher(SalesOrder order) {
        // 查找或创建应收账款科目（资产类）
        Account accountsReceivable = accountRepository.findAll().stream()
                .filter(a -> a.getName().contains("应收账款") && a.getType() == AccountType.ASSET)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("系统中不存在'应收账款'科目，请先创建"));

        // 查找或创建主营业务收入科目（收入类）
        Account salesRevenue = accountRepository.findAll().stream()
                .filter(a -> (a.getName().contains("主营业务收入") || a.getName().contains("销售收入")) && a.getType() == AccountType.INCOME)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("系统中不存在'主营业务收入'科目，请先创建"));

        // 查找或创建主营业务成本科目（支出类）
        Account costOfSales = accountRepository.findAll().stream()
                .filter(a -> (a.getName().contains("主营业务成本") || a.getName().contains("销售成本")) && a.getType() == AccountType.EXPENSE)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("系统中不存在'主营业务成本'科目，请先创建"));

        // 查找或创建库存商品科目（资产类）
        Account inventory = accountRepository.findAll().stream()
                .filter(a -> (a.getName().contains("库存商品") || a.getName().contains("产成品")) && a.getType() == AccountType.ASSET)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("系统中不存在'库存商品'科目，请先创建"));

        // 生成销售收入凭证
        Transaction revenueVoucher = new Transaction();
        revenueVoucher.setDate(LocalDate.now());
        revenueVoucher.setDescription("销售单审核通过 - " + order.getOrderNumber());
        revenueVoucher.setStatus(VoucherStatus.UNAUDITED);

        // 借方：应收账款
        TransactionEntry debitEntry = new TransactionEntry();
        debitEntry.setTransaction(revenueVoucher);
        debitEntry.setAccount(accountsReceivable);
        debitEntry.setDebitCredit(DebitCredit.DEBIT);
        debitEntry.setAmount(order.getReceivableAmount());
        revenueVoucher.getEntries().add(debitEntry);

        // 贷方：主营业务收入
        TransactionEntry creditEntry = new TransactionEntry();
        creditEntry.setTransaction(revenueVoucher);
        creditEntry.setAccount(salesRevenue);
        creditEntry.setDebitCredit(DebitCredit.CREDIT);
        creditEntry.setAmount(order.getReceivableAmount());
        revenueVoucher.getEntries().add(creditEntry);

        transactionRepository.save(revenueVoucher);

        // 生成成本结转凭证（简化处理，假设成本为收入的60%）
        // 实际应用中应该根据商品的实际成本计算
        BigDecimal costAmount = order.getReceivableAmount().multiply(new BigDecimal("0.6"));
        
        Transaction costVoucher = new Transaction();
        costVoucher.setDate(LocalDate.now());
        costVoucher.setDescription("销售成本结转 - " + order.getOrderNumber());
        costVoucher.setStatus(VoucherStatus.UNAUDITED);

        // 借方：主营业务成本
        TransactionEntry costDebitEntry = new TransactionEntry();
        costDebitEntry.setTransaction(costVoucher);
        costDebitEntry.setAccount(costOfSales);
        costDebitEntry.setDebitCredit(DebitCredit.DEBIT);
        costDebitEntry.setAmount(costAmount);
        costVoucher.getEntries().add(costDebitEntry);

        // 贷方：库存商品
        TransactionEntry costCreditEntry = new TransactionEntry();
        costCreditEntry.setTransaction(costVoucher);
        costCreditEntry.setAccount(inventory);
        costCreditEntry.setDebitCredit(DebitCredit.CREDIT);
        costCreditEntry.setAmount(costAmount);
        costVoucher.getEntries().add(costCreditEntry);

        transactionRepository.save(costVoucher);
    }

    /**
     * 发货操作
     */
    @Transactional
    public SalesOrderDto ship(Long orderId, List<ShipmentRequest> shipments) {
        SalesOrder order = salesOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("销售单不存在"));
        
        if (order.getStatus() != SalesOrderStatus.AUDITED && 
            order.getStatus() != SalesOrderStatus.PARTIAL_SHIPMENT) {
            throw new IllegalArgumentException("只能对已审核或部分发货状态的销售单进行发货操作");
        }

        for (ShipmentRequest shipment : shipments) {
            SalesOrderItem item = order.getItems().stream()
                    .filter(i -> i.getId().equals(shipment.getItemId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("订单明细不存在"));

            int shipmentQty = shipment.getQuantity().intValue();
            int newShipped = item.getShippedQuantity() + shipmentQty;
            if (newShipped > item.getQuantity()) {
                throw new IllegalArgumentException("已发货数量不能超过销售数量");
            }

            item.setShippedQuantity(newShipped);
            item.setUnshippedQuantity(item.getQuantity() - newShipped);
        }

        // 更新订单状态
        boolean allShipped = order.getItems().stream()
                .allMatch(item -> item.getShippedQuantity().equals(item.getQuantity()));
        
        if (allShipped) {
            order.setStatus(SalesOrderStatus.SHIPPED);
        } else {
            order.setStatus(SalesOrderStatus.PARTIAL_SHIPMENT);
        }

        SalesOrder saved = salesOrderRepository.save(order);
        return toDto(saved);
    }

    /**
     * 收款操作
     */
    @Transactional
    public SalesOrderDto receivePayment(Long orderId, PaymentRequest request) {
        SalesOrder order = salesOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("销售单不存在"));
        
        if (order.getStatus() != SalesOrderStatus.SHIPPED && 
            order.getStatus() != SalesOrderStatus.PARTIAL_PAYMENT &&
            order.getStatus() != SalesOrderStatus.AUDITED) {
            throw new IllegalArgumentException("只能对已发货、部分收款或已审核状态的销售单进行收款操作");
        }

        BigDecimal newReceived = order.getReceivedAmount().add(request.getAmount());
        if (newReceived.compareTo(order.getReceivableAmount()) > 0) {
            throw new IllegalArgumentException("已收金额不能超过应收总额");
        }

        order.setReceivedAmount(newReceived);
        order.setUnreceivedAmount(order.getReceivableAmount().subtract(newReceived));

        // 更新订单状态
        if (newReceived.compareTo(order.getReceivableAmount()) == 0) {
            order.setStatus(SalesOrderStatus.COMPLETED);
        } else if (order.getReceivedAmount().compareTo(BigDecimal.ZERO) > 0) {
            order.setStatus(SalesOrderStatus.PARTIAL_PAYMENT);
        }

        // 生成收款凭证
        generatePaymentVoucher(order, request.getAmount());

        SalesOrder saved = salesOrderRepository.save(order);
        return toDto(saved);
    }

    /**
     * 生成收款凭证
     */
    private void generatePaymentVoucher(SalesOrder order, BigDecimal amount) {
        // 查找或创建应收账款科目（资产类）
        Account accountsReceivable = accountRepository.findAll().stream()
                .filter(a -> a.getName().contains("应收账款") && a.getType() == AccountType.ASSET)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("系统中不存在'应收账款'科目，请先创建"));

        // 查找或创建银行存款科目（资产类）
        Account bankDeposit = accountRepository.findAll().stream()
                .filter(a -> a.getName().contains("银行存款") && a.getType() == AccountType.ASSET)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("系统中不存在'银行存款'科目，请先创建"));

        Transaction voucher = new Transaction();
        voucher.setDate(LocalDate.now());
        voucher.setDescription("销售单收款 - " + order.getOrderNumber());
        voucher.setStatus(VoucherStatus.UNAUDITED);

        // 借方：银行存款
        TransactionEntry debitEntry = new TransactionEntry();
        debitEntry.setTransaction(voucher);
        debitEntry.setAccount(bankDeposit);
        debitEntry.setDebitCredit(DebitCredit.DEBIT);
        debitEntry.setAmount(amount);
        voucher.getEntries().add(debitEntry);

        // 贷方：应收账款
        TransactionEntry creditEntry = new TransactionEntry();
        creditEntry.setTransaction(voucher);
        creditEntry.setAccount(accountsReceivable);
        creditEntry.setDebitCredit(DebitCredit.CREDIT);
        creditEntry.setAmount(amount);
        voucher.getEntries().add(creditEntry);

        transactionRepository.save(voucher);
    }

    /**
     * 查询所有销售单
     */
    @Transactional(readOnly = true)
    public List<SalesOrderDto> listAll() {
        return salesOrderRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 查询单个销售单
     */
    @Transactional(readOnly = true)
    public SalesOrderDto findById(Long id) {
        SalesOrder order = salesOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("销售单不存在"));
        return toDto(order);
    }

    /**
     * 转换为DTO
     */
    private SalesOrderDto toDto(SalesOrder order) {
        SalesOrderDto dto = new SalesOrderDto();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setCustomerId(order.getCustomer().getId());
        dto.setCustomerName(order.getCustomer().getName());
        dto.setOrderDate(order.getOrderDate());
        dto.setDeliveryDate(order.getDeliveryDate());
        dto.setShippingAddress(order.getShippingAddress());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setDiscountAmount(order.getDiscountAmount());
        dto.setReceivableAmount(order.getReceivableAmount());
        dto.setReceivedAmount(order.getReceivedAmount());
        dto.setUnreceivedAmount(order.getUnreceivedAmount());
        dto.setInvoiceStatus(order.getInvoiceStatus());
        dto.setStatus(order.getStatus());
        if (order.getSubmitter() != null) {
            dto.setSubmitterId(order.getSubmitter().getId());
            dto.setSubmitterName(order.getSubmitter().getUsername());
        }
        dto.setSubmitTime(order.getSubmitTime());
        if (order.getAuditor() != null) {
            dto.setAuditorId(order.getAuditor().getId());
            dto.setAuditorName(order.getAuditor().getUsername());
        }
        dto.setAuditTime(order.getAuditTime());
        dto.setAuditComment(order.getAuditComment());
        
        dto.setItems(order.getItems().stream().map(item -> {
            SalesOrderItemDto itemDto = new SalesOrderItemDto();
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
            itemDto.setDiscountRate(item.getDiscountRate());
            itemDto.setAmount(item.getAmount());
            itemDto.setShippedQuantity(BigDecimal.valueOf(item.getShippedQuantity()));
            itemDto.setUnshippedQuantity(BigDecimal.valueOf(item.getUnshippedQuantity()));
            return itemDto;
        }).collect(Collectors.toList()));
        
        return dto;
    }
}

