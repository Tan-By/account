package com.example.accounting.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 自定义认证入口点
 * 当用户未认证或认证失败时，返回标准的 JSON 格式错误响应（401），而不是重定向到登录页
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException {
        
        System.out.println("🚫 AuthenticationEntryPoint 被触发!");
        System.out.println("   请求路径: " + request.getRequestURI());
        System.out.println("   请求方法: " + request.getMethod());
        System.out.println("   异常信息: " + (authException != null ? authException.getMessage() : "null"));
        
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("message", authException != null ? authException.getMessage() : "未授权：请提供有效的认证令牌");
        body.put("path", request.getRequestURI());

        System.out.println("   返回响应: " + body);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}

