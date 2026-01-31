package com.lumie.auth.infrastructure.multitenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * HTTP filter that extracts the tenant identifier from the X-Tenant-Slug header
 * and sets it in TenantContext for the duration of the request.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantContextFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-Slug";
    private static final String TENANT_SCHEMA_PREFIX = "tenant_";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String tenantSlug = request.getHeader(TENANT_HEADER);

            if (tenantSlug != null && !tenantSlug.isBlank()) {
                String schemaName = TENANT_SCHEMA_PREFIX + tenantSlug;
                TenantContext.setTenantId(schemaName);
                log.debug("Set tenant context to schema: {}", schemaName);
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/") || path.equals("/health");
    }
}
