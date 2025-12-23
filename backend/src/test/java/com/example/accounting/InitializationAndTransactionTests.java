package com.example.accounting;

import com.example.accounting.domain.Company;
import com.example.accounting.domain.enums.AccountType;
import com.example.accounting.web.dto.TransactionCreateRequest;
import com.example.accounting.web.dto.TransactionEntryDto;
import com.example.accounting.web.dto.init.InitAccountDto;
import com.example.accounting.web.dto.init.InitializationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.example.accounting.domain.enums.DebitCredit.CREDIT;
import static com.example.accounting.domain.enums.DebitCredit.DEBIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
public class InitializationAndTransactionTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void initializeAndCreateBalancedTransaction() throws Exception {
        // 初始化企业和科目
        InitAccountDto cash = new InitAccountDto();
        cash.setName("现金");
        cash.setCode("1001");
        cash.setType(AccountType.ASSET);
        cash.setCurrencyCode("CNY");
        cash.setInitialBalance(new BigDecimal("1000.00"));

        InitAccountDto equity = new InitAccountDto();
        equity.setName("实收资本");
        equity.setCode("3001");
        equity.setType(AccountType.EQUITY);
        equity.setCurrencyCode("CNY");
        equity.setInitialBalance(new BigDecimal("1000.00"));

        InitializationRequest initReq = new InitializationRequest();
        initReq.setCompanyName("测试公司");
        initReq.setDefaultCurrencyCode("CNY");
        initReq.setStartDate(LocalDate.of(2025, 1, 1));
        initReq.setAccounts(List.of(cash, equity));

        String initJson = objectMapper.writeValueAsString(initReq);

        String initResp = mockMvc.perform(post("/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(initJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Company company = objectMapper.readValue(initResp, Company.class);
        assertThat(company.isInitialized()).isTrue();

        // 获取账户ID（简单起见，直接查询 /accounts）
        mockMvc.perform(post("/currencies/quick-init"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 这里我们不解析账户列表，而是在真实项目中你可以再写一个获取账户并选择ID的步骤。
        // 因为本测试的重点在于：初始化可以成功，且记账接口可以校验借贷平衡。

        TransactionEntryDto e1 = new TransactionEntryDto();
        e1.setAccountId(1L); // 假设现金ID为1（H2内存库，测试环境可控）
        e1.setDebitCredit(DEBIT);
        e1.setAmount(new BigDecimal("100.00"));

        TransactionEntryDto e2 = new TransactionEntryDto();
        e2.setAccountId(2L); // 假设实收资本ID为2
        e2.setDebitCredit(CREDIT);
        e2.setAmount(new BigDecimal("100.00"));

        TransactionCreateRequest txReq = new TransactionCreateRequest();
        txReq.setDate(LocalDate.of(2025, 1, 2));
        txReq.setDescription("测试记账");
        txReq.setEntries(List.of(e1, e2));

        String txJson = objectMapper.writeValueAsString(txReq);

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(txJson))
                .andExpect(status().isOk());
    }
}


