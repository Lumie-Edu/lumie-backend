package com.lumie.auth.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.auth.application.exception.AuthErrorCode;
import com.lumie.auth.application.port.out.SessionPersistencePort;
import com.lumie.auth.domain.vo.AuthSession;
import com.lumie.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisSessionPersistenceAdapter implements SessionPersistencePort {

    private static final String SESSION_PREFIX = "auth:session:";
    private static final String USER_INDEX_PREFIX = "auth:user-sessions:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void save(AuthSession session) {
        Duration ttl = Duration.between(Instant.now(), session.expiresAt());
        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(sessionKey(session.id()), objectMapper.writeValueAsString(session), ttl);
            String indexKey = indexKey(session.tenantSlug(), session.userId());
            redisTemplate.opsForSet().add(indexKey, session.id());
            redisTemplate.expire(indexKey, ttl);
        } catch (JsonProcessingException e) {
            throw new BusinessException(AuthErrorCode.SESSION_PERSISTENCE_FAILED, e);
        }
    }

    @Override
    public Optional<AuthSession> findById(String sessionId) {
        String value = redisTemplate.opsForValue().get(sessionKey(sessionId));
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, AuthSession.class));
        } catch (JsonProcessingException e) {
            log.warn("Discarding unreadable auth session {}", sessionId, e);
            redisTemplate.delete(sessionKey(sessionId));
            return Optional.empty();
        }
    }

    @Override
    public void delete(AuthSession session) {
        redisTemplate.delete(sessionKey(session.id()));
        redisTemplate.opsForSet().remove(indexKey(session.tenantSlug(), session.userId()), session.id());
    }

    @Override
    public void deleteAllForUser(String tenantSlug, Long userId) {
        String indexKey = indexKey(tenantSlug, userId);
        Set<String> ids = redisTemplate.opsForSet().members(indexKey);
        if (ids != null && !ids.isEmpty()) {
            redisTemplate.delete(ids.stream().map(this::sessionKey).toList());
        }
        redisTemplate.delete(indexKey);
    }

    @Override
    public Set<String> findSessionIds(String tenantSlug, Long userId) {
        Set<String> ids = redisTemplate.opsForSet().members(indexKey(tenantSlug, userId));
        return ids == null ? Collections.emptySet() : ids;
    }

    @Override
    public Map<String, AuthSession> findSessions(String tenantSlug, Long userId, Set<String> sessionIds) {
        if (sessionIds.isEmpty()) {
            return Map.of();
        }
        List<String> ids = sessionIds.stream().toList();
        List<String> values = redisTemplate.opsForValue().multiGet(ids.stream().map(this::sessionKey).toList());
        Map<String, AuthSession> sessions = new HashMap<>();
        for (int i = 0; values != null && i < values.size(); i++) {
            String value = values.get(i);
            if (value == null) {
                redisTemplate.opsForSet().remove(indexKey(tenantSlug, userId), ids.get(i));
                continue;
            }
            try {
                AuthSession session = objectMapper.readValue(value, AuthSession.class);
                if (session.tenantSlug().equals(tenantSlug)) {
                    sessions.put(ids.get(i), session);
                }
            } catch (JsonProcessingException e) {
                log.warn("Discarding unreadable indexed auth session {}", ids.get(i), e);
                redisTemplate.delete(sessionKey(ids.get(i)));
            }
        }
        return sessions;
    }

    private String sessionKey(String sessionId) {
        return SESSION_PREFIX + sessionId;
    }

    private String indexKey(String tenantSlug, Long userId) {
        return USER_INDEX_PREFIX + tenantSlug + ":" + userId;
    }
}
