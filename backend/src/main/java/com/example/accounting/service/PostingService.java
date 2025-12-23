package com.example.accounting.service;

import com.example.accounting.domain.Account;
import com.example.accounting.domain.Transaction;
import com.example.accounting.domain.TransactionEntry;
import com.example.accounting.domain.enums.DebitCredit;
import com.example.accounting.domain.enums.VoucherStatus;
import com.example.accounting.repository.AccountRepository;
import com.example.accounting.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 过账服务
 * 负责将已审核的凭证过账到总账和明细账，并更新科目余额
 */
@Service
public class PostingService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public PostingService(TransactionRepository transactionRepository,
                         AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * 查询待过账的凭证（状态为"已审核"）
     */
    @Transactional(readOnly = true)
    public List<Transaction> getPendingPostingVouchers() {
        return transactionRepository.findByStatus(VoucherStatus.AUDITED);
    }

    /**
     * 执行过账操作
     * @param voucherIds 要过账的凭证ID列表
     * @return 成功过账的凭证数量
     */
    @Transactional
    public int postVouchers(List<Long> voucherIds) {
        if (voucherIds == null || voucherIds.isEmpty()) {
            throw new IllegalArgumentException("请选择要过账的凭证");
        }

        // 查询所有选定的凭证（使用 EntityGraph 预加载关联对象）
        List<Transaction> vouchers = transactionRepository.findAllByIdWithGraph(voucherIds);
        
        if (vouchers.size() != voucherIds.size()) {
            throw new IllegalArgumentException("部分凭证不存在");
        }

        // 校验所有凭证状态必须为"已审核"
        List<Transaction> invalidVouchers = vouchers.stream()
                .filter(v -> v.getStatus() != VoucherStatus.AUDITED)
                .collect(Collectors.toList());
        
        if (!invalidVouchers.isEmpty()) {
            StringBuilder errorMsg = new StringBuilder("选择的凭证中包含未审核凭证，无法过账。不符合条件的凭证：");
            invalidVouchers.forEach(v -> errorMsg.append("凭证#").append(v.getId()).append(" "));
            throw new IllegalArgumentException(errorMsg.toString());
        }

        // 执行过账：更新凭证状态、更新账户余额
        int successCount = 0;
        for (Transaction voucher : vouchers) {
            try {
                // 检查凭证是否有分录
                if (voucher.getEntries() == null || voucher.getEntries().isEmpty()) {
                    throw new IllegalArgumentException("凭证#" + voucher.getId() + "没有分录，无法过账");
                }
                
                // 更新凭证状态为"已过账"
                voucher.setStatus(VoucherStatus.POSTED);
                
                // 将凭证分录数据记入相应的明细账簿（这里通过更新账户余额实现）
                // 更新相关科目的总账余额
                for (TransactionEntry entry : voucher.getEntries()) {
                    if (entry == null) {
                        continue;
                    }
                    
                    Account account = entry.getAccount();
                    if (account == null) {
                        throw new IllegalArgumentException("凭证#" + voucher.getId() + "的分录缺少账户信息");
                    }
                    
                    BigDecimal currentBalance = account.getBalance();
                    BigDecimal amount = entry.getAmount();
                    if (amount == null) {
                        throw new IllegalArgumentException("凭证#" + voucher.getId() + "的分录金额为空");
                    }
                    
                    // 根据借贷方向更新余额
                    // 资产、支出类账户：借方增加，贷方减少
                    // 负债、权益、收入类账户：贷方增加，借方减少
                    if (entry.getDebitCredit() == DebitCredit.DEBIT) {
                        // 借方：资产、支出类增加，负债、权益、收入类减少
                        if (account.getType().name().equals("ASSET") || account.getType().name().equals("EXPENSE")) {
                            account.setBalance(currentBalance.add(amount));
                        } else {
                            account.setBalance(currentBalance.subtract(amount));
                        }
                    } else {
                        // 贷方：负债、权益、收入类增加，资产、支出类减少
                        if (account.getType().name().equals("LIABILITY") || 
                            account.getType().name().equals("EQUITY") || 
                            account.getType().name().equals("INCOME")) {
                            account.setBalance(currentBalance.add(amount));
                        } else {
                            account.setBalance(currentBalance.subtract(amount));
                        }
                    }
                    
                    accountRepository.save(account);
                }
                
                transactionRepository.save(voucher);
                successCount++;
            } catch (Exception e) {
                // 记录错误但继续处理其他凭证
                throw new IllegalArgumentException("过账凭证#" + voucher.getId() + "时出错: " + e.getMessage(), e);
            }
        }

        return successCount;
    }
}




