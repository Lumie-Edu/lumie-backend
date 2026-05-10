package com.lumie.auth.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.auth.application.port.out.TokenPersistencePort;
import com.lumie.auth.domain.entity.AuthToken;
import com.lumie.auth.domain.repository.TokenRepository;
import com.lumie.auth.infrastructure.exception.AuthErrorCode;
import com.lumie.common.auth.Role;
import com.lumie.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisTokenRepository implements TokenRepository, TokenPersistencePort {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String BLACKLIST_PREFIX = "auth:blacklist:";
    private static final String REFRESH_PREFIX = "auth:refresh:";
    private static final String SESSION_SUFFIX = ":session:";
    private static final String SESSION_INDEX_SUFFIX = ":sessions:";
    private static final String OAUTH2_NONCE_PREFIX = "auth:oauth2_nonce:";

    @Override
    public void saveRefreshToken(AuthToken token) {
        String key = REFRESH_PREFIX + token.getUserId() + ":" + token.getJti();
        try {
            String value = objectMapper.writeValueAsString(toRedisToken(token));
            Duration ttl = Duration.ofSeconds(token.getRemainingTtlSeconds());
            redisTemplate.opsForValue().set(key, value, ttl);
            log.debug("Saved refresh token for user: {}", token.getUserId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize refresh token for user={}, jti={}", token.getUserId(), token.getJti(), e);
            throw new BusinessException(AuthErrorCode.TOKEN_PERSISTENCE_FAILED, e);
        }
    }

    @Override
    public Optional<AuthToken> findRefreshToken(Long userId, String jti) {
        String key = REFRESH_PREFIX + userId + ":" + jti;
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return Optional.empty();
        }
        try {
            RedisToken redisToken = objectMapper.readValue(value, RedisToken.class);
            return Optional.of(fromRedisToken(redisToken));
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize token", e);
            return Optional.empty();
        }
    }

    @Override
    public void deleteRefreshToken(Long userId, String jti) {
        String key = REFRESH_PREFIX + userId + ":" + jti;
        redisTemplate.delete(key);
        log.debug("Deleted refresh token for user: {} jti: {}", userId, jti);
    }

    @Override
    public void deleteAllRefreshTokensForUser(Long userId) {
        String pattern = REFRESH_PREFIX + userId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("Deleted {} refresh tokens for user: {}", keys.size(), userId);
        }
    }

    @Override
    public void blacklistToken(String jti, long ttlSeconds) {
        String key = BLACKLIST_PREFIX + jti;
        redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(ttlSeconds));
        log.debug("Blacklisted token: {}", jti);
    }

    @Override
    public boolean isBlacklisted(String jti) {
        String key = BLACKLIST_PREFIX + jti;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public void saveSession(String tenantSlug, String jti, String sessionData, long ttlSeconds) {
        String key = tenantSlug + SESSION_SUFFIX + jti;
        redisTemplate.opsForValue().set(key, sessionData, Duration.ofSeconds(ttlSeconds));
        log.debug("Saved session for tenant: {} jti: {}", tenantSlug, jti);
    }

    @Override
    public Optional<String> findSession(String tenantSlug, String jti) {
        String key = tenantSlug + SESSION_SUFFIX + jti;
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }

    @Override
    public void deleteSession(String tenantSlug, String jti) {
        String key = tenantSlug + SESSION_SUFFIX + jti;
        redisTemplate.delete(key);
        log.debug("Deleted session for tenant: {} jti: {}", tenantSlug, jti);
    }

    private static final long SESSION_INDEX_TTL_SECONDS = 604800;

    @Override
    public void addSessionToIndex(String tenantSlug, Long userId, String jti) {
        String key = tenantSlug + SESSION_INDEX_SUFFIX + userId;
        redisTemplate.opsForSet().add(key, jti);
        redisTemplate.expire(key, Duration.ofSeconds(SESSION_INDEX_TTL_SECONDS));
        log.debug("Added session {} to index for user: {}", jti, userId);
    }

    @Override
    public Set<String> findAllSessionJtis(String tenantSlug, Long userId) {
        String key = tenantSlug + SESSION_INDEX_SUFFIX + userId;
        Set<String> members = redisTemplate.opsForSet().members(key);
        return members != null ? members : Collections.emptySet();
    }

    @Override
    public void removeSessionFromIndex(String tenantSlug, Long userId, String jti) {
        String key = tenantSlug + SESSION_INDEX_SUFFIX + userId;
        redisTemplate.opsForSet().remove(key, jti);
        log.debug("Removed session {} from index for user: {}", jti, userId);
    }

    @Override
    public void deleteAllSessionsForUser(String tenantSlug, Long userId) {
        String indexKey = tenantSlug + SESSION_INDEX_SUFFIX + userId;
        Set<String> jtis = redisTemplate.opsForSet().members(indexKey);
        if (jtis != null && !jtis.isEmpty()) {
            List<String> sessionKeys = jtis.stream()
                    .map(jti -> tenantSlug + SESSION_SUFFIX + jti)
                    .toList();
            redisTemplate.delete(sessionKeys);
            log.debug("Deleted {} session keys for user: {}", sessionKeys.size(), userId);
        }
        redisTemplate.delete(indexKey);
        log.debug("Deleted session index for user: {}", userId);
    }

    @Override
    public Map<String, String> findSessionsBulk(String tenantSlug, Set<String> jtis) {
        if (jtis.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> jtiList = new ArrayList<>(jtis);
        List<String> keys = jtiList.stream()
                .map(jti -> tenantSlug + SESSION_SUFFIX + jti)
                .toList();
        List<String> values = redisTemplate.opsForValue().multiGet(keys);
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < jtiList.size(); i++) {
            String value = values != null ? values.get(i) : null;
            if (value != null) {
                result.put(jtiList.get(i), value);
            }
        }
        return result;
    }

    @Override
    public void saveOAuth2Nonce(String nonce, String tenantSlug, long ttlSeconds) {
        String key = OAUTH2_NONCE_PREFIX + nonce;
        redisTemplate.opsForValue().set(key, tenantSlug, Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public Optional<String> consumeOAuth2Nonce(String nonce) {
        String key = OAUTH2_NONCE_PREFIX + nonce;
        return Optional.ofNullable(redisTemplate.opsForValue().getAndDelete(key));
    }

    private RedisToken toRedisToken(AuthToken token) {
        return new RedisToken(
                token.getJti(),
                token.getUserId(),
                token.getTenantSlug(),
                token.getTenantId(),
                token.getRole().name(),
                token.getIssuedAt().toEpochMilli(),
                token.getExpiresAt().toEpochMilli()
        );
    }

    private AuthToken fromRedisToken(RedisToken redisToken) {
        return AuthToken.builder()
                .jti(redisToken.jti())
                .userId(redisToken.userId())
                .tenantSlug(redisToken.tenantSlug())
                .tenantId(redisToken.tenantId())
                .role(Role.fromString(redisToken.role()))
                .issuedAt(Instant.ofEpochMilli(redisToken.issuedAt()))
                .expiresAt(Instant.ofEpochMilli(redisToken.expiresAt()))
                .revoked(false)
                .build();
    }

    private record RedisToken(
            String jti,
            Long userId,
            String tenantSlug,
            Long tenantId,
            String role,
            long issuedAt,
            long expiresAt
    ) {
    }
}
