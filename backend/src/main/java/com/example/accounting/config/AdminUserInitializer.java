package com.example.accounting.config;

import com.example.accounting.domain.Role;
import com.example.accounting.domain.UserAccount;
import com.example.accounting.repository.RoleRepository;
import com.example.accounting.repository.UserAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminUserInitializer implements CommandLineRunner {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserInitializer(UserAccountRepository userAccountRepository,
                                RoleRepository roleRepository,
                                PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // 初始化所有预定义角色
        initializeRoles();
        
        // 检查admin用户是否已存在
        if (userAccountRepository.findByUsername("admin").isEmpty()) {
            // 获取管理员角色
            Role adminRole = roleRepository.findByName("系统管理员")
                    .orElseThrow(() -> new RuntimeException("系统管理员角色未找到"));

            // 创建admin用户
            UserAccount admin = new UserAccount();
            admin.setName("管理员");
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setStatus("启用");
            admin.getRoles().add(adminRole);

            userAccountRepository.save(admin);
            System.out.println("Admin用户已创建: admin/admin123");
        }
    }
    
    private void initializeRoles() {
        // 定义所有预定义角色
        String[][] roles = {
            {"系统管理员", "系统管理员，拥有所有权限"},
            {"财务经理", "财务经理，负责财务管理和审核"},
            {"财务专员", "财务专员，负责日常财务操作"},
            {"出纳", "出纳，负责现金和银行账户管理"},
            {"销售员", "销售员，负责销售相关业务"}
        };
        
        for (String[] roleData : roles) {
            String roleName = roleData[0];
            String description = roleData[1];
            
            roleRepository.findByName(roleName)
                    .orElseGet(() -> {
                        Role role = new Role();
                        role.setName(roleName);
                        role.setDescription(description);
                        return roleRepository.save(role);
                    });
        }
        
        System.out.println("预定义角色已初始化");
    }
}

