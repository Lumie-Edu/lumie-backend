package com.lumie.auth.application.port.out;

import com.lumie.auth.domain.vo.AuthSession;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Persistence boundary for opaque server-side browser sessions. */
public interface SessionPersistencePort {

    void save(AuthSession session);

    Optional<AuthSession> findById(String sessionId);

    void delete(AuthSession session);

    void deleteAllForUser(String tenantSlug, Long userId);

    Set<String> findSessionIds(String tenantSlug, Long userId);

    Map<String, AuthSession> findSessions(String tenantSlug, Long userId, Set<String> sessionIds);
}
