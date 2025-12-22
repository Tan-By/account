package com.example.accounting;

import com.example.accounting.web.dto.tax.TaxDeclarationRequest;
import com.example.accounting.web.dto.user.UserDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class TaxAndUserTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void taxDeclarationFlowAndUserManagement() throws Exception {
        // 税务申报草稿
        TaxDeclarationRequest taxReq = new TaxDeclarationRequest();
        taxReq.setTaxType("增值税");
        taxReq.setPeriodStart(LocalDate.of(2025, 1, 1));
        taxReq.setPeriodEnd(LocalDate.of(2025, 1, 31));

        String taxJson = objectMapper.writeValueAsString(taxReq);

        mockMvc.perform(post("/api/tax-declarations/draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taxJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 简化：不解析返回体，直接假设ID=1，测试接口连通性
        mockMvc.perform(post("/api/tax-declarations/1/submit"))
                .andExpect(status().isOk());

        // 用户管理：创建用户
        UserDto user = new UserDto();
        user.setName("管理员");
        user.setUsername("admin");
        user.setPassword("password");
        user.setRoles(Set.of("SYSTEM_ADMIN"));

        String userJson = objectMapper.writeValueAsString(user);

        String userResp = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserDto created = objectMapper.readValue(userResp, UserDto.class);

        // 更新用户信息
        created.setDepartment("财务部");
        String updateJson = objectMapper.writeValueAsString(created);

        mockMvc.perform(put("/api/users/" + created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk());

        // 禁用用户
        mockMvc.perform(post("/api/users/" + created.getId() + "/disable"))
                .andExpect(status().isNoContent());

        // 查询用户列表
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk());
    }
}


