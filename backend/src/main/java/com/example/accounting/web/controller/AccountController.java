package com.example.accounting.web.controller;

import com.example.accounting.domain.Account;
import com.example.accounting.domain.Currency;
import com.example.accounting.repository.AccountRepository;
import com.example.accounting.repository.CurrencyRepository;
import com.example.accounting.web.dto.AccountDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accounts")
@CrossOrigin
public class AccountController {

    private final AccountRepository accountRepository;
    private final CurrencyRepository currencyRepository;

    public AccountController(AccountRepository accountRepository,
                             CurrencyRepository currencyRepository) {
        this.accountRepository = accountRepository;
        this.currencyRepository = currencyRepository;
    }

    @GetMapping
    public List<AccountDto> list() {
        return accountRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<AccountDto> create(@Valid @RequestBody AccountDto dto) {
        Account account = new Account();
        applyDto(dto, account);
        Account saved = accountRepository.save(account);
        return ResponseEntity.ok(toDto(saved));
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
        return ResponseEntity.ok(toDto(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!accountRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        accountRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private AccountDto toDto(Account account) {
        AccountDto dto = new AccountDto();
        dto.setId(account.getId());
        dto.setName(account.getName());
        dto.setCode(account.getCode());
        dto.setType(account.getType());
        dto.setParentId(account.getParent() != null ? account.getParent().getId() : null);
        dto.setCurrencyCode(account.getCurrency().getCode());
        dto.setBalance(account.getBalance());
        dto.setEnabled(account.isEnabled());
        return dto;
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


