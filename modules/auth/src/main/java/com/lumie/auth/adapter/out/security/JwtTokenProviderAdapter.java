package com.lumie.auth.adapter.out.security;

import com.lumie.auth.application.port.out.TokenProviderPort;
import com.lumie.auth.domain.vo.TokenClaims;
import com.lumie.auth.infrastructure.security.JwtTokenProvider;
import com.lumie.common.auth.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adapter exposing {@link JwtTokenProvider} as {@link TokenProviderPort}.
 * Pure delegation — exists so the application layer can depend on the port
 * instead of the infrastructure class directly.
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProviderAdapter implements TokenProviderPort {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public String generateAccessToken(Long userId, String tenantSlug, Long tenantId,
                                      Role role, String jti, String sid) {
        return jwtTokenProvider.generateAccessToken(userId, tenantSlug, tenantId, role, jti, sid);
    }

    @Override
    public String generateRefreshToken(Long userId, String tenantSlug, Long tenantId,
                                       Role role, String jti, String sid) {
        return jwtTokenProvider.generateRefreshToken(userId, tenantSlug, tenantId, role, jti, sid);
    }

    @Override
    public Optional<TokenClaims> parseToken(String token) {
        return jwtTokenProvider.parseToken(token);
    }

    @Override
    public long getAccessTokenExpiration() {
        return jwtTokenProvider.getAccessTokenExpiration();
    }

    @Override
    public long getRefreshTokenExpiration() {
        return jwtTokenProvider.getRefreshTokenExpiration();
    }
}
