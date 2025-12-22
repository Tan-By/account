package com.example.accounting.config;

import com.example.accounting.domain.Account;
import com.example.accounting.domain.Currency;
import com.example.accounting.domain.enums.AccountType;
import com.example.accounting.repository.AccountRepository;
import com.example.accounting.repository.CurrencyRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
@Order(2) // 在AdminUserInitializer之后执行，确保Currency可能已初始化
public class AccountInitializer implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final CurrencyRepository currencyRepository;

    public AccountInitializer(AccountRepository accountRepository,
                             CurrencyRepository currencyRepository) {
        this.accountRepository = accountRepository;
        this.currencyRepository = currencyRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // 确保CNY货币存在
        Currency cny = currencyRepository.findByCode("CNY")
                .orElseGet(() -> {
                    Currency currency = new Currency();
                    currency.setCode("CNY");
                    currency.setName("人民币");
                    currency.setFractionDigits(2);
                    return currencyRepository.save(currency);
                });

        // 如果已有账户，则不初始化（避免重复初始化）
        if (accountRepository.count() > 0) {
            return;
        }

        // 用于存储已创建的账户，key为code，value为Account对象
        Map<String, Account> accountMap = new HashMap<>();

        // 定义预设科目数据
        AccountData[] accounts = {
            // 资产类
            new AccountData("1001", "库存现金", AccountType.ASSET, null),
            new AccountData("1002", "银行存款", AccountType.ASSET, null),
            new AccountData("1122", "应收账款", AccountType.ASSET, null),
            new AccountData("1221", "其他应收款", AccountType.ASSET, null),
            new AccountData("1401", "材料采购", AccountType.ASSET, null),
            new AccountData("1403", "原材料", AccountType.ASSET, null),
            new AccountData("1405", "库存商品", AccountType.ASSET, null),
            new AccountData("1601", "固定资产", AccountType.ASSET, null),
            new AccountData("1602", "累计折旧", AccountType.ASSET, "1601"),
            new AccountData("1801", "长期待摊费用", AccountType.ASSET, null),
            // 负债类
            new AccountData("2001", "短期借款", AccountType.LIABILITY, null),
            new AccountData("2201", "应付票据", AccountType.LIABILITY, null),
            new AccountData("2202", "应付账款", AccountType.LIABILITY, null),
            new AccountData("2211", "应付职工薪酬", AccountType.LIABILITY, null),
            new AccountData("2221", "应交税费", AccountType.LIABILITY, null),
            new AccountData("2231", "应付利息", AccountType.LIABILITY, null),
            new AccountData("2241", "其他应付款", AccountType.LIABILITY, null),
            new AccountData("2501", "长期借款", AccountType.LIABILITY, null),
            // 权益类
            new AccountData("4001", "实收资本", AccountType.EQUITY, null),
            new AccountData("4002", "资本公积", AccountType.EQUITY, null),
            new AccountData("4101", "盈余公积", AccountType.EQUITY, null),
            new AccountData("4103", "本年利润", AccountType.EQUITY, null),
            new AccountData("4104", "利润分配", AccountType.EQUITY, null),
            // 收入类
            new AccountData("6001", "主营业务收入", AccountType.INCOME, null),
            new AccountData("6051", "其他业务收入", AccountType.INCOME, null),
            new AccountData("6111", "投资收益", AccountType.INCOME, null),
            new AccountData("6301", "营业外收入", AccountType.INCOME, null),
            // 费用类
            new AccountData("6401", "主营业务成本", AccountType.EXPENSE, null),
            new AccountData("6402", "其他业务成本", AccountType.EXPENSE, null),
            new AccountData("6403", "税金及附加", AccountType.EXPENSE, null),
            new AccountData("6601", "销售费用", AccountType.EXPENSE, null),
            new AccountData("6602", "管理费用", AccountType.EXPENSE, null),
            new AccountData("6603", "财务费用", AccountType.EXPENSE, null),
            new AccountData("6711", "营业外支出", AccountType.EXPENSE, null),
            new AccountData("6801", "所得税费用", AccountType.EXPENSE, null)
        };

        // 第一遍：创建所有账户（不设置parent）
        for (AccountData data : accounts) {
            Account account = new Account();
            account.setCode(data.code);
            account.setName(data.name);
            account.setType(data.type);
            account.setCurrency(cny);
            account.setBalance(BigDecimal.ZERO);
            account.setEnabled(true);
            Account saved = accountRepository.save(account);
            accountMap.put(data.code, saved);
        }

        // 第二遍：设置parent关系
        for (AccountData data : accounts) {
            if (data.parentCode != null) {
                Account account = accountMap.get(data.code);
                Account parent = accountMap.get(data.parentCode);
                if (account != null && parent != null) {
                    account.setParent(parent);
                    accountRepository.save(account);
                }
            }
        }

        System.out.println("预设科目已初始化，共 " + accounts.length + " 个科目");
    }

    // 内部类用于存储账户数据
    private static class AccountData {
        String code;
        String name;
        AccountType type;
        String parentCode; // 使用code而不是id来引用parent

        AccountData(String code, String name, AccountType type, String parentCode) {
            this.code = code;
            this.name = name;
            this.type = type;
            this.parentCode = parentCode;
        }
    }
}

