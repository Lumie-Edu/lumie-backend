package com.lumie.auth.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.auth.application.port.out.TokenPersistencePort;
import com.lumie.auth.domain.entity.AuthToken;
import com.lumie.auth.domain.repository.TokenRepository;
import com.lumie.auth.domain.vo.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
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

    @Override
    public void saveRefreshToken(AuthToken token) {
        String key = REFRESH_PREFIX + token.getUserId() + ":" + token.getJti();
        try {
            String value = objectMapper.writeValueAsString(toRedisToken(token));
            Duration ttl = Duration.ofSeconds(token.getRemainingTtlSeconds());
            redisTemplate.opsForValue().set(key, value, ttl);
            log.debug("Saved refresh token for user: {}", token.getUserId());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize token", e);
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
