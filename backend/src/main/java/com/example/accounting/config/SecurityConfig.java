package com.example.accounting.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                        UserDetailsService userDetailsService,
                        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许所有源（使用 setAllowedOriginPatterns 可以配合 allowCredentials）
        // 生产环境建议配置具体域名，例如：List.of("https://yourdomain.com", "https://www.yourdomain.com")
        configuration.setAllowedOriginPatterns(List.of("*"));
        // 允许所有请求头（包括 Nginx 代理添加的头部，如 X-Forwarded-For, X-Real-IP 等）
        configuration.setAllowedHeaders(List.of("*"));
        // 允许所有 HTTP 方法（包括 OPTIONS 预检请求）
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        // 允许携带凭证（Cookie、Authorization 等）
        // 注意：如果前端不需要发送 Cookie，可以设置为 false
        configuration.setAllowCredentials(true);
        // 预检请求的缓存时间（秒），减少 OPTIONS 请求频率
        configuration.setMaxAge(3600L);
        // 允许暴露的响应头（前端可以访问的响应头）
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        System.out.println("🔧 SecurityConfig: 开始配置 SecurityFilterChain");
        
        http
                // 彻底禁用 CSRF（无状态后端不需要，Stateful 架构才需要）
                .csrf(csrf -> csrf.disable())
                
                // 配置 CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                // 设置无状态 Session 管理（确保后端不保存任何会话）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // 配置请求授权 - 白名单必须在 anyRequest().authenticated() 之前定义
                .authorizeHttpRequests(auth -> auth
                        // 认证相关接口（登录、注册等）- Nginx 会去掉 /api/ 前缀，所以路径是 /auth/**
                        .requestMatchers("/auth/**").permitAll()
                        // 错误处理接口
                        .requestMatchers("/error", "/error/**").permitAll()
                        // 静态资源（CSS、JS、图片等）
                        .requestMatchers("/static/**", "/public/**", "/resources/**", "/webjars/**").permitAll()
                        // API 文档相关
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**", "/swagger-resources/**").permitAll()
                        // H2 控制台（开发环境）
                        .requestMatchers("/h2-console/**").permitAll()
                        // 所有其他请求都需要认证
                        .anyRequest().authenticated()
                )
                
                // 配置认证提供者
                .authenticationProvider(authenticationProvider())
                
                // 配置异常处理入口点（返回 JSON 格式的 401 错误）
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                
                // H2 控制台需要允许同源框架
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                
                // 请求日志过滤器最先执行（用于调试）
                // JWT 过滤器在 UsernamePasswordAuthenticationFilter 之前执行
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        System.out.println("✅ SecurityConfig: SecurityFilterChain 配置完成");
        return http.build();
    }
}


