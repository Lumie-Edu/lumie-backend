package com.lumie.common.tenant;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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
 *   <li>{@link RequestContextFilter} - extracts tenant/user context from headers</li>
 *   <li>{@link TenantIdentifierResolver} - Hibernate multi-tenancy (when Hibernate is present)</li>
 * </ul>
 *
 * <p>Configuration properties:
 * <pre>
 * lumie:
 *   tenant:
 *     enabled: true              # Enable tenant context (default: true)
 *     hibernate-enabled: true    # Enable Hibernate resolver (default: true)
 *     exclude-paths:             # Paths excluded from tenant check
 *       - /actuator/**
 *       - /health/**
 * </pre>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "lumie.tenant", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(TenantProperties.class)
public class TenantAutoConfiguration {

    @Bean
    public FilterRegistrationBean<RequestContextFilter> requestContextFilter(TenantProperties properties) {
        FilterRegistrationBean<RequestContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequestContextFilter(properties.getExcludePaths()));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.setName("requestContextFilter");
        return registration;
    }

    @Bean
    @ConditionalOnClass(name = "org.hibernate.context.spi.CurrentTenantIdentifierResolver")
    @ConditionalOnProperty(prefix = "lumie.tenant", name = "hibernate-enabled", matchIfMissing = true)
    public TenantIdentifierResolver tenantIdentifierResolver() {
        return new TenantIdentifierResolver();
    }
}
