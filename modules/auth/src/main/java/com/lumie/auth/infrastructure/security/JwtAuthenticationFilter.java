package com.lumie.auth.infrastructure.security;

import com.lumie.auth.application.service.AuthQueryService;
import com.lumie.auth.domain.vo.TokenClaims;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthQueryService authQueryService;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ACCESS_TOKEN_COOKIE = "lumie_access_token";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null) {
            Optional<TokenClaims> claimsOpt = authQueryService.validate(token);

            if (claimsOpt.isPresent()) {
                TokenClaims claims = claimsOpt.get();

                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + claims.role().name())
                );

                AuthenticatedUser principal = new AuthenticatedUser(
                        claims.getUserId(),
                        claims.tenantSlug(),
                        claims.tenantId(),
                        claims.role()
                );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);

                TenantContextHolder.setTenant(claims.tenantSlug());
                TenantContextHolder.setTenantId(claims.tenantId());
                UserContextHolder.setUserId(claims.getUserId());
                UserContextHolder.setUserName(claims.name());
                UserContextHolder.setUserRole(claims.role().name());
                UserContextHolder.setCurrentSid(claims.sid());

                log.debug("Authenticated user: {} for tenant: {}", claims.sub(), claims.tenantSlug());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/v1/login") ||
                path.startsWith("/v1/register") ||
                path.startsWith("/v1/refresh") ||
                path.startsWith("/v1/oauth2") ||
                path.startsWith("/actuator");
    }
}
