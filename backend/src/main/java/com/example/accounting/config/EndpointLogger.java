package com.example.accounting.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 应用启动时打印所有注册的 HTTP 端点
 */
@Component
public class EndpointLogger implements ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        RequestMappingHandlerMapping mapping = event.getApplicationContext()
                .getBean(RequestMappingHandlerMapping.class);

        Map<org.springframework.web.servlet.mvc.method.RequestMappingInfo, org.springframework.web.method.HandlerMethod> handlerMethods = mapping.getHandlerMethods();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("📋 已注册的 HTTP 端点列表:");
        System.out.println("=".repeat(80));

        List<String> endpoints = new ArrayList<>();

        handlerMethods.forEach((info, method) -> {
            String handler = method.getBeanType().getSimpleName() + "." + method.getMethod().getName();
            
            // 安全地获取路径模式，处理 null 情况
            if (info.getPatternsCondition() != null && info.getPatternsCondition().getPatterns() != null) {
                info.getPatternsCondition().getPatterns().forEach(pattern -> {
                    // 安全地获取 HTTP 方法
                    if (info.getMethodsCondition() != null && info.getMethodsCondition().getMethods() != null) {
                        info.getMethodsCondition().getMethods().forEach(httpMethod -> {
                            endpoints.add(String.format("  %-8s %-50s -> %s", 
                                httpMethod.name(), 
                                pattern, 
                                handler));
                        });
                    }
                    // 如果没有指定方法，显示所有方法
                    if (info.getMethodsCondition() == null || 
                        info.getMethodsCondition().getMethods() == null || 
                        info.getMethodsCondition().getMethods().isEmpty()) {
                        endpoints.add(String.format("  %-8s %-50s -> %s", 
                            "ALL", 
                            pattern, 
                            handler));
                    }
                });
            } else {
                // 如果无法获取路径信息，至少显示处理器信息
                endpoints.add(String.format("  %-8s %-50s -> %s", 
                    "UNKNOWN", 
                    "N/A", 
                    handler));
            }
        });

        // 按路径排序
        endpoints.sort(Comparator.naturalOrder());

        endpoints.forEach(System.out::println);

        System.out.println("=".repeat(80));
        System.out.println("✅ 共注册 " + endpoints.size() + " 个端点\n");
    }
}

