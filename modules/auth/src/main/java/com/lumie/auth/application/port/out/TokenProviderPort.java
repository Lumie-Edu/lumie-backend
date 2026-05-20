package com.lumie.auth.application.port.out;

import com.lumie.auth.domain.vo.TokenClaims;
import com.lumie.common.auth.Role;

import java.util.Optional;

/**
 * Outgoing port for issuing and parsing access/refresh tokens.
 *
 * <p>Lets the application layer talk to a token-generation contract instead
 * of pulling in the {@link com.lumie.auth.adapter.out.security.JwtTokenProvider}
 * adapter class. The implementation is an adapter that delegates to
 * the JWT provider; tests can swap a fake.
 */
public interface TokenProviderPort {

    String generateAccessToken(Long userId, String name, String tenantSlug, Long tenantId,
                               Role role, String jti, String sid);

    String generateRefreshToken(Long userId, String name, String tenantSlug, Long tenantId,
                                Role role, String jti, String sid);

    Optional<TokenClaims> parseToken(String token);

    /** @return access-token TTL in seconds */
    long getAccessTokenExpiration();

    /** @return refresh-token TTL in seconds */
    long getRefreshTokenExpiration();
}
