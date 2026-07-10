package com.lumie.auth.application.port.out;

import com.lumie.auth.domain.entity.AuthToken;
import com.lumie.auth.domain.vo.SessionRevocationReason;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

    void addSessionToIndex(String tenantSlug, Long userId, String jti);

    Set<String> findAllSessionJtis(String tenantSlug, Long userId);

    void removeSessionFromIndex(String tenantSlug, Long userId, String jti);

    void deleteAllSessionsForUser(String tenantSlug, Long userId);

    Map<String, String> findSessionsBulk(String tenantSlug, Set<String> jtis);

    void saveSessionRevocationReason(String tenantSlug, String sid, SessionRevocationReason reason, long ttlSeconds);

    Optional<SessionRevocationReason> findSessionRevocationReason(String tenantSlug, String sid);
}
