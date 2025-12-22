package com.example.accounting.service;

import com.example.accounting.domain.TaxDeclaration;
import com.example.accounting.repository.TaxDeclarationRepository;
import com.example.accounting.web.dto.tax.TaxDeclarationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class TaxDeclarationService {

    private final TaxDeclarationRepository taxDeclarationRepository;

    public TaxDeclarationService(TaxDeclarationRepository taxDeclarationRepository) {
        this.taxDeclarationRepository = taxDeclarationRepository;
    }

    /**
     * 生成申报草稿（简化：用随机或固定值模拟从报表汇总）
     */
    @Transactional
    public TaxDeclaration createDraft(TaxDeclarationRequest request) {
        TaxDeclaration d = new TaxDeclaration();
        d.setTaxType(request.getTaxType());
        d.setPeriodStart(request.getPeriodStart());
        d.setPeriodEnd(request.getPeriodEnd());
        d.setStatus("草稿");

        // 这里简化处理：真实情况应根据报表数据计算
        BigDecimal taxable = new BigDecimal("10000.00");
        BigDecimal taxRate = new BigDecimal("0.13"); // 比如13%增值税
        BigDecimal taxPayable = taxable.multiply(taxRate);

        d.setTaxableAmount(taxable);
        d.setTaxPayable(taxPayable);
        d.setTaxRelief(BigDecimal.ZERO);
        d.setTaxPaid(BigDecimal.ZERO);
        d.setTaxDue(taxPayable);

        return taxDeclarationRepository.save(d);
    }

    @Transactional
    public TaxDeclaration submit(Long id) {
        TaxDeclaration d = taxDeclarationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("申报记录不存在"));
        if (!"草稿".equals(d.getStatus())) {
            throw new IllegalStateException("只有草稿状态才能提交");
        }
        d.setStatus("已提交");
        d.setSubmittedAt(LocalDateTime.now());
        return taxDeclarationRepository.save(d);
    }

    @Transactional
    public TaxDeclaration markAccepted(Long id) {
        TaxDeclaration d = taxDeclarationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("申报记录不存在"));
        d.setStatus("已受理");
        return taxDeclarationRepository.save(d);
    }

    @Transactional
    public TaxDeclaration markSuccess(Long id) {
        TaxDeclaration d = taxDeclarationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("申报记录不存在"));
        d.setStatus("申报成功");
        return taxDeclarationRepository.save(d);
    }
}


