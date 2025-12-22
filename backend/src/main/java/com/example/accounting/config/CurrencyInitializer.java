package com.example.accounting.config;

import com.example.accounting.domain.Currency;
import com.example.accounting.domain.ExchangeRate;
import com.example.accounting.repository.CurrencyRepository;
import com.example.accounting.repository.ExchangeRateRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Component
@Order(1) // 在AccountInitializer之前执行
public class CurrencyInitializer implements CommandLineRunner {

    private final CurrencyRepository currencyRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    public CurrencyInitializer(CurrencyRepository currencyRepository,
                              ExchangeRateRepository exchangeRateRepository) {
        this.currencyRepository = currencyRepository;
        this.exchangeRateRepository = exchangeRateRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 定义货币数据：代码、名称、小数位数
        Map<String, CurrencyData> currencies = new HashMap<>();
        currencies.put("CNY", new CurrencyData("CNY", "人民币", 2));
        currencies.put("USD", new CurrencyData("USD", "美元", 2));
        currencies.put("EUR", new CurrencyData("EUR", "欧元", 2));
        currencies.put("JPY", new CurrencyData("JPY", "日元", 0));

        // 初始化货币
        Map<String, Currency> currencyMap = new HashMap<>();
        for (CurrencyData data : currencies.values()) {
            Currency currency = currencyRepository.findByCode(data.code)
                    .orElseGet(() -> {
                        Currency c = new Currency();
                        c.setCode(data.code);
                        c.setName(data.name);
                        c.setFractionDigits(data.fractionDigits);
                        return currencyRepository.save(c);
                    });
            currencyMap.put(data.code, currency);
        }

        // 定义固定汇率（以CNY为基准，1 CNY = ? 其他币种）
        // 使用符合常识的固定汇率
        Map<String, BigDecimal> ratesToCNY = new HashMap<>();
        ratesToCNY.put("USD", new BigDecimal("0.14"));  // 1 CNY = 0.14 USD (约7.14 CNY = 1 USD)
        ratesToCNY.put("EUR", new BigDecimal("0.13"));  // 1 CNY = 0.13 EUR (约7.69 CNY = 1 EUR)
        ratesToCNY.put("JPY", new BigDecimal("20.0"));  // 1 CNY = 20 JPY (约0.05 CNY = 1 JPY)

        LocalDate today = LocalDate.now();

        // 初始化汇率（双向）
        // CNY -> 其他币种
        for (Map.Entry<String, BigDecimal> entry : ratesToCNY.entrySet()) {
            String targetCode = entry.getKey();
            BigDecimal rate = entry.getValue();
            Currency from = currencyMap.get("CNY");
            Currency to = currencyMap.get(targetCode);

            // 检查是否已存在
            boolean exists = exchangeRateRepository.countByCurrencyCodesAndDate("CNY", targetCode, today) > 0;

            if (!exists) {
                ExchangeRate rate1 = new ExchangeRate();
                rate1.setFromCurrency(from);
                rate1.setToCurrency(to);
                rate1.setRate(rate);
                rate1.setRateDate(today);
                exchangeRateRepository.save(rate1);

                // 反向汇率
                ExchangeRate rate2 = new ExchangeRate();
                rate2.setFromCurrency(to);
                rate2.setToCurrency(from);
                rate2.setRate(BigDecimal.ONE.divide(rate, 8, RoundingMode.HALF_UP));
                rate2.setRateDate(today);
                exchangeRateRepository.save(rate2);
            }
        }

        // 其他币种之间的汇率（通过CNY中转）
        // USD -> EUR: USD -> CNY -> EUR
        createCrossRate(currencyMap, "USD", "EUR", 
                ratesToCNY.get("EUR").divide(ratesToCNY.get("USD"), 8, RoundingMode.HALF_UP), today);
        createCrossRate(currencyMap, "EUR", "USD", 
                ratesToCNY.get("USD").divide(ratesToCNY.get("EUR"), 8, RoundingMode.HALF_UP), today);
        
        // USD -> JPY
        createCrossRate(currencyMap, "USD", "JPY", 
                ratesToCNY.get("JPY").divide(ratesToCNY.get("USD"), 8, RoundingMode.HALF_UP), today);
        createCrossRate(currencyMap, "JPY", "USD", 
                ratesToCNY.get("USD").divide(ratesToCNY.get("JPY"), 8, RoundingMode.HALF_UP), today);
        
        // EUR -> JPY
        createCrossRate(currencyMap, "EUR", "JPY", 
                ratesToCNY.get("JPY").divide(ratesToCNY.get("EUR"), 8, RoundingMode.HALF_UP), today);
        createCrossRate(currencyMap, "JPY", "EUR", 
                ratesToCNY.get("EUR").divide(ratesToCNY.get("JPY"), 8, RoundingMode.HALF_UP), today);

        System.out.println("货币和汇率已初始化：CNY, USD, EUR, JPY");
    }

    private void createCrossRate(Map<String, Currency> currencyMap, String fromCode, String toCode, 
                                 BigDecimal rate, LocalDate date) {
        boolean exists = exchangeRateRepository.countByCurrencyCodesAndDate(fromCode, toCode, date) > 0;

        if (!exists) {
            ExchangeRate rateObj = new ExchangeRate();
            rateObj.setFromCurrency(currencyMap.get(fromCode));
            rateObj.setToCurrency(currencyMap.get(toCode));
            rateObj.setRate(rate);
            rateObj.setRateDate(date);
            exchangeRateRepository.save(rateObj);
        }
    }

    private static class CurrencyData {
        String code;
        String name;
        int fractionDigits;

        CurrencyData(String code, String name, int fractionDigits) {
            this.code = code;
            this.name = name;
            this.fractionDigits = fractionDigits;
        }
    }
}

