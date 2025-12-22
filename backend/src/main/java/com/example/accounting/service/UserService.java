package com.example.accounting.service;

import com.example.accounting.domain.Role;
import com.example.accounting.domain.UserAccount;
import com.example.accounting.repository.RoleRepository;
import com.example.accounting.repository.UserAccountRepository;
import com.example.accounting.web.dto.user.UserDto;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserAccountRepository userAccountRepository,
                       RoleRepository roleRepository) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
    }

    public List<UserDto> list() {
        return userAccountRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDto create(UserDto dto) {
        userAccountRepository.findByUsername(dto.getUsername())
                .ifPresent(u -> {
                    throw new IllegalArgumentException("用户名已存在");
                });
        UserAccount u = new UserAccount();
        apply(dto, u, true);
        return toDto(userAccountRepository.save(u));
    }

    @Transactional
    public UserDto update(Long id, UserDto dto) {
        UserAccount u = userAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (!u.getUsername().equals(dto.getUsername())) {
            userAccountRepository.findByUsername(dto.getUsername())
                    .ifPresent(exist -> {
                        throw new IllegalArgumentException("用户名已存在");
                    });
        }
        apply(dto, u, false);
        return toDto(userAccountRepository.save(u));
    }

    @Transactional
    public void disable(Long id) {
        UserAccount u = userAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        u.setStatus("禁用");
        userAccountRepository.save(u);
    }

    private UserDto toDto(UserAccount u) {
        UserDto dto = new UserDto();
        dto.setId(u.getId());
        dto.setName(u.getName());
        dto.setUsername(u.getUsername());
        dto.setEmployeeNo(u.getEmployeeNo());
        dto.setDepartment(u.getDepartment());
        dto.setPosition(u.getPosition());
        dto.setStatus(u.getStatus());
        dto.setRoles(u.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
        return dto;
    }

    private void apply(UserDto dto, UserAccount u, boolean isCreate) {
        u.setName(dto.getName());
        u.setUsername(dto.getUsername());
        u.setEmployeeNo(dto.getEmployeeNo());
        u.setDepartment(dto.getDepartment());
        u.setPosition(dto.getPosition());
        if (dto.getStatus() != null) {
            u.setStatus(dto.getStatus());
        }
        if (dto.getPassword() != null) {
            u.setPassword(passwordEncoder.encode(dto.getPassword()));
        } else if (isCreate) {
            throw new IllegalArgumentException("创建用户必须提供初始密码");
        }

        Set<Role> roles = new HashSet<>();
        if (dto.getRoles() != null) {
            for (String roleName : dto.getRoles()) {
                Role r = roleRepository.findByName(roleName)
                        .orElseGet(() -> {
                            Role nr = new Role();
                            nr.setName(roleName);
                            return roleRepository.save(nr);
                        });
                roles.add(r);
            }
        }
        u.setRoles(roles);
    }
}


