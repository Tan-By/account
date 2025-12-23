package com.example.accounting;

import com.example.accounting.domain.Account;
import com.example.accounting.domain.BankStatementRecord;
import com.example.accounting.domain.Currency;
import com.example.accounting.domain.enums.AccountType;
import com.example.accounting.repository.AccountRepository;
import com.example.accounting.repository.BankStatementRecordRepository;
import com.example.accounting.repository.CurrencyRepository;
import com.example.accounting.web.dto.reconciliation.ReconciliationRequest;
import com.example.accounting.web.dto.report.ReportRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
public class ReportAndReconciliationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private BankStatementRecordRepository bankStatementRecordRepository;

    private Account bankAccount;

    @BeforeEach
    void setup() {
        Currency cny = currencyRepository.findByCode("CNY").orElseGet(() -> {
            Currency c = new Currency();
            c.setCode("CNY");
            c.setName("人民币");
            c.setFractionDigits(2);
            return currencyRepository.save(c);
        });

        if (bankAccount == null) {
            bankAccount = new Account();
            bankAccount.setName("银行存款");
            bankAccount.setType(AccountType.ASSET);
            bankAccount.setCurrency(cny);
            bankAccount.setBalance(new BigDecimal("1000.00"));
            bankAccount.setEnabled(true);
            bankAccount = accountRepository.save(bankAccount);
        }

        bankStatementRecordRepository.deleteAll();

        BankStatementRecord br = new BankStatementRecord();
        br.setBankAccount(bankAccount);
        br.setDate(LocalDate.of(2025, 1, 5));
        br.setDescription("对账测试");
        br.setDebitAmount(new BigDecimal("100.00"));
        br.setBalance(new BigDecimal("1100.00"));
        bankStatementRecordRepository.save(br);
    }

    @Test
    void generateReportsAndReconcile() throws Exception {
        ReportRequest profitReq = new ReportRequest();
        profitReq.setType("PROFIT");
        profitReq.setStartDate(LocalDate.of(2025, 1, 1));
        profitReq.setEndDate(LocalDate.of(2025, 1, 31));
        profitReq.setCurrencyCode("CNY");

        String profitJson = objectMapper.writeValueAsString(profitReq);

        mockMvc.perform(post("/reports/profit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profitJson))
                .andExpect(status().isOk());

        ReportRequest bsReq = new ReportRequest();
        bsReq.setType("BALANCE_SHEET");
        bsReq.setStartDate(LocalDate.of(2025, 1, 1));
        bsReq.setEndDate(LocalDate.of(2025, 1, 31));
        bsReq.setCurrencyCode("CNY");

        String bsJson = objectMapper.writeValueAsString(bsReq);

        mockMvc.perform(post("/reports/balance-sheet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bsJson))
                .andExpect(status().isOk());

        ReconciliationRequest recReq = new ReconciliationRequest();
        recReq.setBankAccountId(bankAccount.getId());
        recReq.setPeriodStart(LocalDate.of(2025, 1, 1));
        recReq.setPeriodEnd(LocalDate.of(2025, 1, 31));
        recReq.setExecutor("tester");

        String recJson = objectMapper.writeValueAsString(recReq);

        mockMvc.perform(post("/reconciliations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recJson))
                .andExpect(status().isOk());
    }
}


