package com.example.accounting;

import com.example.accounting.web.dto.ExternalContactDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ExternalContactTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createAndDeactivateContact() throws Exception {
        ExternalContactDto dto = new ExternalContactDto();
        dto.setName("供应商A");
        dto.setType("供应商");
        dto.setTaxId("TAX123");

        String json = objectMapper.writeValueAsString(dto);

        String resp = mockMvc.perform(post("/api/external-contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ExternalContactDto created = objectMapper.readValue(resp, ExternalContactDto.class);

        // 停用
        mockMvc.perform(post("/api/external-contacts/" + created.getId() + "/deactivate"))
                .andExpect(status().isNoContent());

        // 再次用相同税号创建联系人（前一个已停用），应允许成功创建新的活跃联系人
        mockMvc.perform(post("/api/external-contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }
}


