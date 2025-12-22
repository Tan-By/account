package com.example.accounting.web.controller;

import com.example.accounting.domain.UserAccount;
import com.example.accounting.repository.UserAccountRepository;
import com.example.accounting.util.JwtUtil;
import com.example.accounting.web.dto.auth.LoginRequest;
import com.example.accounting.web.dto.auth.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final UserAccountRepository userAccountRepository;

    public AuthController(AuthenticationManager authenticationManager,
                         UserDetailsService userDetailsService,
                         JwtUtil jwtUtil,
                         UserAccountRepository userAccountRepository) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.userAccountRepository = userAccountRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        System.out.println("收到登录请求: " + request.getUsername());
        try {
            // 验证用户凭证
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            System.out.println("认证失败: " + e.getMessage());
            throw new IllegalArgumentException("用户名或密码错误");
        }

        // 加载用户详情
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());

        // 生成JWT token
        String token = jwtUtil.generateToken(userDetails.getUsername());

        // 获取用户信息
        UserAccount userAccount = userAccountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        return ResponseEntity.ok(new LoginResponse(token, userAccount.getUsername(), userAccount.getName()));
    }
}

