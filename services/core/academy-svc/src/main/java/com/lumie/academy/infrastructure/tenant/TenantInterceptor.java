package com.lumie.academy.infrastructure.tenant;

import com.lumie.academy.application.port.out.AuthServicePort;
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

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthServicePort authServicePort;
    private final TenantServicePort tenantServicePort;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Extract token from Authorization header
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("Missing or invalid Authorization header for request: {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        // Validate token and extract claims via gRPC to auth-svc
        var claimsOpt = authServicePort.validateToken(token);
        if (claimsOpt.isEmpty()) {
            log.warn("Invalid or expired token for request: {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        var claims = claimsOpt.get();

        // Validate tenant is still active
        if (!tenantServicePort.validateTenant(claims.tenantSlug())) {
            log.warn("Invalid or inactive tenant: {}", claims.tenantSlug());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        // Set context for the request
        TenantContextHolder.setTenant(claims.tenantSlug());
        UserContextHolder.setUserId(claims.userId());

        log.debug("Request authenticated: userId={}, tenant={}", claims.userId(), claims.tenantSlug());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        TenantContextHolder.clear();
        UserContextHolder.clear();
    }
}
