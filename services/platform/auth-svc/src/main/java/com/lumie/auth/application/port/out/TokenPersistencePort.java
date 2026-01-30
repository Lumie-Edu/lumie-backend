package com.lumie.auth.application.port.out;

import com.lumie.auth.domain.entity.AuthToken;

import java.util.Optional;

/**
 * Output port for token persistence operations (Redis).
 */
public interface TokenPersistencePort {

    void saveRefreshToken(AuthToken token);

    Optional<AuthToken> findRefreshToken(Long userId, String jti);

    void deleteRefreshToken(Long userId, String jti);

    void deleteAllRefreshTokensForUser(Long userId);

    void blacklistToken(String jti, long ttlSeconds);

    boolean isBlacklisted(String jti);

    void saveSession(String tenantSlug, String jti, String sessionData, long ttlSeconds);

    Optional<String> findSession(String tenantSlug, String jti);

    void deleteSession(String tenantSlug, String jti);
}
