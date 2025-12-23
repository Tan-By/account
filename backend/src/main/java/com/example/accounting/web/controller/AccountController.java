package com.example.accounting.web.controller;

import com.example.accounting.domain.Account;
import com.example.accounting.domain.Currency;
import com.example.accounting.domain.ExchangeRate;
import com.example.accounting.repository.AccountRepository;
import com.example.accounting.repository.CurrencyRepository;
import com.example.accounting.repository.ExchangeRateRepository;
import com.example.accounting.web.dto.AccountDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/accounts")
@CrossOrigin
public class AccountController {

    private final AccountRepository accountRepository;
    private final CurrencyRepository currencyRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    public AccountController(AccountRepository accountRepository,
                             CurrencyRepository currencyRepository,
                             ExchangeRateRepository exchangeRateRepository) {
        this.accountRepository = accountRepository;
        this.currencyRepository = currencyRepository;
        this.exchangeRateRepository = exchangeRateRepository;
    }

    @GetMapping
    public List<AccountDto> list(@RequestParam(required = false) String displayCurrency) {
        Currency displayCurr = null;
        if (displayCurrency != null && !displayCurrency.isEmpty()) {
            displayCurr = currencyRepository.findByCode(displayCurrency)
                    .orElse(null);
        }
        
        final Currency finalDisplayCurr = displayCurr;
        LocalDate today = LocalDate.now();
        
        return accountRepository.findAll()
                .stream()
                .map(acc -> toDto(acc, finalDisplayCurr, today))
                .collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<AccountDto> create(@Valid @RequestBody AccountDto dto) {
        Account account = new Account();
        applyDto(dto, account);
        Account saved = accountRepository.save(account);
        return ResponseEntity.ok(toDto(saved, null, LocalDate.now()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountDto> update(@PathVariable Long id,
                                             @Valid @RequestBody AccountDto dto) {
        Optional<Account> optional = accountRepository.findById(id);
        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Account account = optional.get();
        applyDto(dto, account);
        Account saved = accountRepository.save(account);
        return ResponseEntity.ok(toDto(saved, null, LocalDate.now()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!accountRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        accountRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private AccountDto toDto(Account account, Currency displayCurrency, LocalDate date) {
        AccountDto dto = new AccountDto();
        dto.setId(account.getId());
        dto.setName(account.getName());
        dto.setCode(account.getCode());
        dto.setType(account.getType());
        dto.setParentId(account.getParent() != null ? account.getParent().getId() : null);
        dto.setCurrencyCode(account.getCurrency().getCode());
        
        // 如果指定了显示币种，则转换余额
        if (displayCurrency != null && !account.getCurrency().getId().equals(displayCurrency.getId())) {
            BigDecimal convertedBalance = convert(account.getBalance(), account.getCurrency(), displayCurrency, date);
            dto.setBalance(convertedBalance);
        } else {
            dto.setBalance(account.getBalance());
        }
        
        dto.setEnabled(account.isEnabled());
        return dto;
    }

    private BigDecimal convert(BigDecimal amount, Currency from, Currency to, LocalDate date) {
        if (from.getId().equals(to.getId()) || amount.compareTo(BigDecimal.ZERO) == 0) {
            return amount;
        }
        Optional<ExchangeRate> rateOpt = exchangeRateRepository
                .findTopByFromCurrencyAndToCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(from, to, date);
        if (rateOpt.isEmpty()) {
            // 如果找不到汇率，返回原金额（避免报错）
            return amount;
        }
        return amount.multiply(rateOpt.get().getRate());
    }

    private void applyDto(AccountDto dto, Account account) {
        account.setName(dto.getName());
        account.setCode(dto.getCode());
        account.setType(dto.getType());
        account.setEnabled(dto.isEnabled());

        if (dto.getParentId() != null) {
            accountRepository.findById(dto.getParentId()).ifPresent(account::setParent);
        } else {
            account.setParent(null);
        }

        Currency currency = currencyRepository.findByCode(dto.getCurrencyCode())
                .orElseThrow(() -> new IllegalArgumentException("Invalid currency code: " + dto.getCurrencyCode()));
        account.setCurrency(currency);
    }
}


