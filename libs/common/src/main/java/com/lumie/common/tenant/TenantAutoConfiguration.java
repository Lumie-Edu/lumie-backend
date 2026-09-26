package com.lumie.common.tenant;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * Auto-configuration for multi-tenancy support.
 *
 * <p>Automatically registers:
 * <ul>
 *   <li>{@link RequestContextFilter} - adds correlation and authenticated user context</li>
 * </ul>
 *
 * <p>RLS-based tenancy (post-2026-05-12): per-tx tenant binding is handled
 * by {@link RlsTenantContextAspect}, which is a regular {@code @Component}
 * picked up by component scan — no auto-configuration entry needed here.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "lumie.tenant", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(TenantProperties.class)
public class TenantAutoConfiguration {

    @Bean
    public FilterRegistrationBean<RequestContextFilter> tenantRequestContextFilterRegistration(TenantProperties properties) {
        FilterRegistrationBean<RequestContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequestContextFilter(properties.getExcludePaths()));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.setName("tenantRequestContextFilter");
        return registration;
    }

    @Bean
    @ConditionalOnProperty(prefix = "lumie.logging", name = "access-log-enabled", matchIfMissing = true)
    public FilterRegistrationBean<AccessLogFilter> accessLogFilterRegistration(TenantProperties properties) {
        FilterRegistrationBean<AccessLogFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AccessLogFilter(properties.getExcludePaths()));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 11);
        registration.setName("accessLogFilter");
        return registration;
    }
}
