package com.lumie.tenant.adapter.in.web;

import com.lumie.common.tenant.RequestContextFilter;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.tenant.application.service.HostTenantResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/** Binds the addressed tenant from the request Host before authentication. */
@Component
@RequiredArgsConstructor
public class HostTenantFilter extends OncePerRequestFilter {

    private final HostTenantResolver resolver;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/internal/") || path.startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String host = request.getHeader(HttpHeaders.HOST);
        if (host == null || host.isBlank()) {
            host = request.getServerName();
        }
        // Server-side calls use the ClusterIP service host and carry their
        // tenant context through the internal HMAC contract.
        if (host.endsWith(".svc") || host.contains(".svc:")) {
            filterChain.doFilter(request, response);
            return;
        }
        final Optional<HostTenantResolver.ResolvedTenant> resolved;
        try {
            resolved = resolver.resolve(host);
        } catch (DataAccessException | IllegalStateException exception) {
            response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value());
            return;
        }

        if (resolved.isEmpty()) {
            if (!resolver.isRootHost(host)) {
                response.sendError(HttpStatus.NOT_FOUND.value());
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        HostTenantResolver.ResolvedTenant tenant = resolved.get();
        if (!tenant.isActive()) {
            response.sendError(HttpStatus.FORBIDDEN.value());
            return;
        }

        request.setAttribute(RequestContextFilter.HOST_TENANT_SLUG_ATTRIBUTE, tenant.tenant().getSlugValue());
        request.setAttribute(RequestContextFilter.HOST_TENANT_ID_ATTRIBUTE, tenant.tenant().getId());
        request.setAttribute(RequestContextFilter.HOST_TENANT_RESOLUTION_ATTRIBUTE, tenant);
        TenantContextHolder.setTenant(tenant.tenant().getSlugValue());
        TenantContextHolder.setTenantId(tenant.tenant().getId());
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
