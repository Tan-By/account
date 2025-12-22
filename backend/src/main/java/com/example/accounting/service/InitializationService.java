package com.example.accounting.service;

import com.example.accounting.domain.Account;
import com.example.accounting.domain.Company;
import com.example.accounting.domain.Currency;
import com.example.accounting.domain.enums.AccountType;
import com.example.accounting.repository.AccountRepository;
import com.example.accounting.repository.CompanyRepository;
import com.example.accounting.repository.CurrencyRepository;
import com.example.accounting.web.dto.init.InitAccountDto;
import com.example.accounting.web.dto.init.InitializationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class InitializationService {

    private final CompanyRepository companyRepository;
    private final AccountRepository accountRepository;
    private final CurrencyRepository currencyRepository;

    public InitializationService(CompanyRepository companyRepository,
                                 AccountRepository accountRepository,
                                 CurrencyRepository currencyRepository) {
        this.companyRepository = companyRepository;
        this.accountRepository = accountRepository;
        this.currencyRepository = currencyRepository;
    }

    @Transactional
    public Company initialize(InitializationRequest request) {
        companyRepository.findTopByOrderByIdAsc().ifPresent(c -> {
            if (c.isInitialized()) {
                throw new IllegalStateException("系统已初始化，不能重复初始化");
            }
        });

        Currency defaultCurrency = currencyRepository.findByCode(request.getDefaultCurrencyCode())
                .orElseGet(() -> {
                    Currency c = new Currency();
                    c.setCode(request.getDefaultCurrencyCode());
                    c.setName(request.getDefaultCurrencyCode());
                    c.setFractionDigits(2);
                    return currencyRepository.save(c);
                });

        Company company = companyRepository.findTopByOrderByIdAsc()
                .orElseGet(Company::new);
        company.setName(request.getCompanyName());
        company.setTaxId(request.getTaxId());
        company.setAccountingStandard(request.getAccountingStandard());
        company.setDefaultCurrency(defaultCurrency);
        company.setStartDate(request.getStartDate());

        // 先保存公司基础信息
        company = companyRepository.save(company);

        // 创建科目并校验平衡
        Map<String, Account> codeMap = new HashMap<>();
        for (InitAccountDto dto : request.getAccounts()) {
            Account account = new Account();
            account.setName(dto.getName());
            account.setCode(dto.getCode());
            account.setType(dto.getType());
            Currency currency = currencyRepository.findByCode(dto.getCurrencyCode())
                    .orElseThrow(() -> new IllegalArgumentException("不存在的币种: " + dto.getCurrencyCode()));
            account.setCurrency(currency);
            account.setBalance(dto.getInitialBalance());
            account.setEnabled(true);
            Account saved = accountRepository.save(account);
            if (dto.getCode() != null) {
                codeMap.put(dto.getCode(), saved);
            }
        }

        // 处理父子关系（基于 parentCode）
        for (InitAccountDto dto : request.getAccounts()) {
            if (dto.getParentCode() != null && dto.getCode() != null) {
                Account child = codeMap.get(dto.getCode());
                Account parent = codeMap.get(dto.getParentCode());
                if (child != null && parent != null) {
                    child.setParent(parent);
                    accountRepository.save(child);
                }
            }
        }

        // 校验期初平衡：资产 = 负债 + 所有者权益
        BigDecimal asset = BigDecimal.ZERO;
        BigDecimal liability = BigDecimal.ZERO;
        BigDecimal equity = BigDecimal.ZERO;
        for (InitAccountDto dto : request.getAccounts()) {
            AccountType t = dto.getType();
            BigDecimal bal = dto.getInitialBalance();
            switch (t) {
                case ASSET -> asset = asset.add(bal);
                case LIABILITY -> liability = liability.add(bal);
                case EQUITY -> equity = equity.add(bal);
                case INCOME, EXPENSE -> {
                    if (bal.compareTo(BigDecimal.ZERO) != 0) {
                        throw new IllegalArgumentException("损益类科目期初余额必须为0: " + dto.getName());
                    }
                }
            }
        }
        if (asset.compareTo(liability.add(equity)) != 0) {
            throw new IllegalArgumentException("期初不平衡：资产(" + asset + ") ≠ 负债+权益(" + liability.add(equity) + ")");
        }

        company.setInitialized(true);
        return companyRepository.save(company);
    }
}


