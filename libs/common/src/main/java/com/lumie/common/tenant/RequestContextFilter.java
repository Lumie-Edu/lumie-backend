package com.lumie.common.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filter that extracts tenant and user context from request headers
 * and stores them in ThreadLocal holders.
 *
 * <p>Kong JWT plugin validates the token and sets these headers:
 * <ul>
 *   <li>X-User-Id: User ID from JWT sub claim</li>
 *   <li>X-Tenant-Slug: Tenant slug from JWT tenant_slug claim</li>
 *   <li>X-User-Role: User role from JWT role claim</li>
 * </ul>
 */
public class RequestContextFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestContextFilter.class);

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String TENANT_SLUG_HEADER = "X-Tenant-Slug";
    public static final String USER_ROLE_HEADER = "X-User-Role";

    private final List<String> excludePaths;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RequestContextFilter(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return excludePaths.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String tenantSlug = request.getHeader(TENANT_SLUG_HEADER);
            String userId = request.getHeader(USER_ID_HEADER);
            String userRole = request.getHeader(USER_ROLE_HEADER);

            if (tenantSlug == null || tenantSlug.isBlank()) {
                log.warn("Missing {} header for request: {}", TENANT_SLUG_HEADER, request.getRequestURI());
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing tenant slug header");
                return;
            }

            TenantContextHolder.setTenant(tenantSlug);

            if (userId != null && !userId.isBlank()) {
                try {
                    UserContextHolder.setUserId(Long.parseLong(userId));
                } catch (NumberFormatException e) {
                    log.error("Invalid {} header value: {}", USER_ID_HEADER, userId);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid user ID format");
                    return;
                }
            }

            if (userRole != null && !userRole.isBlank()) {
                UserContextHolder.setUserRole(userRole);
            }

            log.debug("Context set: tenant={}, userId={}, role={}",
                    tenantSlug, userId, userRole);

            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
            UserContextHolder.clear();
        }
    }
}
