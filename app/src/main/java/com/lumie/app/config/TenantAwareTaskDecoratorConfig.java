package com.lumie.app.config;

import com.lumie.common.auth.Role;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * Captures request-thread {@link TenantContextHolder} / {@link UserContextHolder}
 * / {@link MDC} state and replays it on the worker thread of any auto-configured
 * Spring {@code TaskExecutor}.
 *
 * <p>Spring Boot's {@code TaskExecutionAutoConfiguration} wires any single
 * {@link TaskDecorator} bean into the default {@code ThreadPoolTaskExecutor},
 * so {@code @Async} methods and {@code @ApplicationModuleListener} dispatches
 * (when async-mode is enabled later) inherit tenant/user context without each
 * call site re-establishing it.
 *
 * <p>Without this bean, ThreadLocal context would not propagate and
 * downstream code calling {@code TenantContextHolder.getRequiredTenant()}
 * would fail — the issue flagged by ARCHITECTURE_AUDIT 3.4.
 */
@Configuration
public class TenantAwareTaskDecoratorConfig {

    @Bean
    public TaskDecorator tenantAwareTaskDecorator() {
        return runnable -> {
            String tenantSlug = TenantContextHolder.getTenant();
            Long tenantId = TenantContextHolder.getTenantId();
            Long userId = UserContextHolder.getUserId();
            String userName = UserContextHolder.getUserName();
            Role userRole = UserContextHolder.getUserRole();
            String sid = UserContextHolder.getCurrentSid();
            Map<String, String> mdc = MDC.getCopyOfContextMap();
            org.springframework.security.core.context.SecurityContext securityContext = org.springframework.security.core.context.SecurityContextHolder.getContext();

            return () -> {
                try {
                    if (tenantSlug != null) TenantContextHolder.setTenant(tenantSlug);
                    if (tenantId != null) TenantContextHolder.setTenantId(tenantId);
                    if (userId != null) UserContextHolder.setUserId(userId);
                    if (userName != null) UserContextHolder.setUserName(userName);
                    if (userRole != null) UserContextHolder.setUserRole(userRole);
                    if (sid != null) UserContextHolder.setCurrentSid(sid);
                    if (mdc != null) MDC.setContextMap(mdc);
                    org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);

                    runnable.run();
                } finally {
                    TenantContextHolder.clear();
                    UserContextHolder.clear();
                    MDC.clear();
                    org.springframework.security.core.context.SecurityContextHolder.clearContext();
                }
            };
        };
    }
}
