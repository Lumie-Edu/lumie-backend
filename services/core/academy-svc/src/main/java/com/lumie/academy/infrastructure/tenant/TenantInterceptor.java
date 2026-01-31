package com.lumie.academy.infrastructure.tenant;

import com.lumie.academy.application.port.out.TenantServicePort;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

    private static final String TENANT_HEADER = "X-Tenant-Slug";

    private final TenantServicePort tenantServicePort;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantSlug = request.getHeader(TENANT_HEADER);

        if (tenantSlug == null || tenantSlug.isBlank()) {
            log.warn("Missing tenant header for request: {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }

        if (!tenantServicePort.validateTenant(tenantSlug)) {
            log.warn("Invalid tenant: {}", tenantSlug);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        TenantContextHolder.setTenant(tenantSlug);
        log.debug("Tenant context set: {}", tenantSlug);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        TenantContextHolder.clear();
    }
}
