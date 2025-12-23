package com.example.accounting.web.controller;

import com.example.accounting.domain.Currency;
import com.example.accounting.repository.CurrencyRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/currencies")
@CrossOrigin
public class CurrencyController {

    private final CurrencyRepository currencyRepository;

    public CurrencyController(CurrencyRepository currencyRepository) {
        this.currencyRepository = currencyRepository;
    }

    @GetMapping
    public List<Currency> list() {
        return currencyRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<Currency> create(@RequestBody Currency currency) {
        if (currency.getId() != null) {
            currency.setId(null);
        }
        return ResponseEntity.ok(currencyRepository.save(currency));
    }

    @PostMapping("/quick-init")
    public ResponseEntity<Void> quickInit() {
        if (currencyRepository.count() == 0) {
            Currency cny = new Currency();
            cny.setCode("CNY");
            cny.setName("人民币");
            cny.setFractionDigits(2);
            currencyRepository.save(cny);

            Currency usd = new Currency();
            usd.setCode("USD");
            usd.setName("美元");
            usd.setFractionDigits(2);
            currencyRepository.save(usd);
        }
        return ResponseEntity.noContent().build();
    }
}


