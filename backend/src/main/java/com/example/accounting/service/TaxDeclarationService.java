package com.example.accounting.service;

import com.example.accounting.domain.Currency;
import com.example.accounting.domain.ExchangeRate;
import com.example.accounting.domain.TaxDeclaration;
import com.example.accounting.repository.CurrencyRepository;
import com.example.accounting.repository.ExchangeRateRepository;
import com.example.accounting.repository.TaxDeclarationRepository;
import com.example.accounting.web.dto.tax.TaxDeclarationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class TaxDeclarationService {

    private final TaxDeclarationRepository taxDeclarationRepository;
    private final CurrencyRepository currencyRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    public TaxDeclarationService(TaxDeclarationRepository taxDeclarationRepository,
                                 CurrencyRepository currencyRepository,
                                 ExchangeRateRepository exchangeRateRepository) {
        this.taxDeclarationRepository = taxDeclarationRepository;
        this.currencyRepository = currencyRepository;
        this.exchangeRateRepository = exchangeRateRepository;
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
        
        // 如果指定了显示币种，则转换金额（假设原始金额是CNY）
        Currency displayCurr = null;
        if (request.getDisplayCurrency() != null && !request.getDisplayCurrency().isEmpty()) {
            displayCurr = currencyRepository.findByCode(request.getDisplayCurrency()).orElse(null);
        }
        if (displayCurr != null) {
            Currency cny = currencyRepository.findByCode("CNY").orElse(null);
            if (cny != null && !cny.getId().equals(displayCurr.getId())) {
                LocalDate today = LocalDate.now();
                taxable = convert(taxable, cny, displayCurr, today);
                taxPayable = convert(taxPayable, cny, displayCurr, today);
            }
        }

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
    
    private BigDecimal convert(BigDecimal amount, Currency from, Currency to, LocalDate date) {
        if (from.getId().equals(to.getId()) || amount.compareTo(BigDecimal.ZERO) == 0) {
            return amount;
        }
        Optional<ExchangeRate> rateOpt = exchangeRateRepository
                .findTopByFromCurrencyAndToCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(from, to, date);
        if (rateOpt.isEmpty()) {
            return amount;
        }
        return amount.multiply(rateOpt.get().getRate());
    }
}


