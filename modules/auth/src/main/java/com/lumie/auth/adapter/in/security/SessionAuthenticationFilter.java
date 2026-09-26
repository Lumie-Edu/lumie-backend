package com.lumie.auth.adapter.in.security;

import com.lumie.auth.application.service.AuthQueryService;
import com.lumie.auth.adapter.out.config.SessionConfig;
import com.lumie.auth.domain.vo.AuthSession;
import com.lumie.common.tenant.RequestContextFilter;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Authenticates browser requests from one opaque, host-only session cookie. */
@Component
@RequiredArgsConstructor
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private final AuthQueryService authQueryService;
    private final SessionConfig sessionConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String sessionId = extractSessionId(request);
        if (sessionId != null) {
            AuthSession session = authQueryService.findActiveSession(sessionId).orElse(null);
            if (session != null) {
                Object hostTenant = request.getAttribute(RequestContextFilter.HOST_TENANT_SLUG_ATTRIBUTE);
                if (hostTenant instanceof String hostTenantSlug && !hostTenantSlug.equals(session.tenantSlug())) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tenant context mismatch");
                    return;
                }
                authenticate(session);
            }
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(AuthSession session) {
        AuthenticatedUser principal = new AuthenticatedUser(
                session.userId(), session.tenantSlug(), session.tenantId(), session.role());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + session.role().name()))));
        TenantContextHolder.setTenant(session.tenantSlug());
        TenantContextHolder.setTenantId(session.tenantId());
        UserContextHolder.setUserId(session.userId());
        UserContextHolder.setUserName(session.userName());
        UserContextHolder.setUserRole(session.role());
        UserContextHolder.setCurrentSid(session.id());
    }

    private String extractSessionId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (sessionConfig.getCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/v1/login") || path.startsWith("/v1/register/owner")
                || path.startsWith("/actuator") || path.startsWith("/internal/");
    }
}
