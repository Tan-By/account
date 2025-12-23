package com.example.accounting.config;

import com.example.accounting.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * JWT 认证过滤器
 * 
 * 核心逻辑：
 * 1. 如果请求路径属于白名单，直接放行（不进行 JWT 验证）
 * 2. 如果 Authorization Header 为空或格式不正确，直接放行，让 Spring Security 的最终决策层处理
 * 3. 只有当 Authorization Header 存在且格式正确时，才进行 JWT 验证
 * 4. 验证成功后设置 SecurityContext，验证失败则继续过滤链（不抛出异常）
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    
    // 白名单路径列表（这些路径不需要 JWT 验证）
    // 注意：Nginx 会去掉 /api/ 前缀，所以后端收到的路径不包含 /api
    private static final List<String> WHITELIST_PATHS = Arrays.asList(
            "/auth/",  // 认证相关接口（登录、注册等）
            "/error",
            "/static/",
            "/public/",
            "/resources/",
            "/webjars/",
            "/v3/api-docs/",
            "/swagger-ui",
            "/swagger-resources/",
            "/h2-console/"
    );

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    /**
     * 检查请求路径是否在白名单中
     */
    private boolean isWhitelistedPath(String requestPath) {
        if (requestPath == null) {
            return false;
        }
        boolean isWhitelisted = WHITELIST_PATHS.stream().anyMatch(requestPath::startsWith);
        if (isWhitelisted) {
            System.out.println("✅ 路径在白名单中: " + requestPath);
        }
        return isWhitelisted;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // 使用 getServletPath() 获取路径（Nginx 去掉 /api/ 前缀后的路径）
        String servletPath = request.getServletPath();
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        
        System.out.println("🔍 JWT Filter - ServletPath: " + servletPath + ", RequestURI: " + requestURI + ", 方法: " + method);
        
        // 关键：如果路径以 /auth/ 开头，直接放行，不进行 Token 校验
        if (servletPath != null && servletPath.startsWith("/auth/")) {
            System.out.println("✅ JWT Filter - /auth/ 路径，直接放行: " + servletPath);
            chain.doFilter(request, response);
            return;
        }
        
        // 检查其他白名单路径
        if (isWhitelistedPath(servletPath)) {
            System.out.println("✅ JWT Filter - 白名单路径，直接放行: " + servletPath);
            chain.doFilter(request, response);
            return;
        }
        
        System.out.println("⚠️ JWT Filter - 非白名单路径，继续处理: " + servletPath);

        // 获取 Authorization Header
        String authHeader = request.getHeader("Authorization");
        
        // 如果 Authorization Header 为空或格式不正确，直接放行
        // 让 Spring Security 的最终决策层处理（会触发 AuthenticationEntryPoint 返回 401）
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        // 提取 JWT Token
        String jwt = authHeader.substring(7);
        String username = null;

        // 尝试从 Token 中提取用户名
        try {
            username = jwtUtil.extractUsername(jwt);
        } catch (Exception e) {
            // Token 格式无效或已过期，继续过滤链
            // 不抛出异常，让 Spring Security 的最终决策层处理
            chain.doFilter(request, response);
            return;
        }

        // 如果成功提取用户名且当前 SecurityContext 中没有认证信息，则进行验证
        if (StringUtils.hasText(username) && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                // 验证 Token 是否有效
                if (jwtUtil.validateToken(jwt, userDetails.getUsername())) {
                    // 创建认证令牌并设置到 SecurityContext
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception e) {
                // 用户不存在或 Token 无效，继续过滤链
                // 不抛出异常，让 Spring Security 的最终决策层处理
            }
        }

        // 继续过滤链
        chain.doFilter(request, response);
    }
}

