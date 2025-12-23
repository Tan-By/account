package com.example.accounting;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import com.example.accounting.config.JwtAuthenticationFilter;
import com.example.accounting.config.JwtAuthenticationEntryPoint;

/**
 * 测试环境安全配置
 * 在测试中禁用 Spring Security，允许所有请求通过
 * Mock 掉 SecurityConfig 依赖的 Bean，避免加载主配置
 */
@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

    // Mock SecurityConfig 依赖的 Bean，避免加载主配置时出错
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}

