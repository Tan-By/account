package com.example.accounting.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 请求日志过滤器 - 记录所有进入的请求
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        String queryString = request.getQueryString();
        String fullPath = queryString != null ? requestURI + "?" + queryString : requestURI;
        
        System.out.println("🌐 收到请求: " + method + " " + fullPath);
        System.out.println("   完整 URL: " + request.getRequestURL().toString());
        System.out.println("   Context Path: " + request.getContextPath());
        System.out.println("   Servlet Path: " + request.getServletPath());
        System.out.println("   Path Info: " + request.getPathInfo());
        
        chain.doFilter(request, response);
    }
}

