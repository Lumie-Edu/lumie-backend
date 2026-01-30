package com.lumie.auth.domain.entity;

import com.lumie.auth.domain.vo.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Domain entity representing an authentication token.
 * This is not a JPA entity as tokens are stored in Redis.
 */
@Getter
@Builder
public class AuthToken {

    private final String jti;
    private final Long userId;
    private final String tenantSlug;
    private final Long tenantId;
    private final Role role;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private final boolean revoked;

    public static AuthToken create(String jti, Long userId, String tenantSlug,
                                    Long tenantId, Role role, Instant expiresAt) {
        return AuthToken.builder()
                .jti(jti)
                .userId(userId)
                .tenantSlug(tenantSlug)
                .tenantId(tenantId)
                .role(role)
                .issuedAt(Instant.now())
                .expiresAt(expiresAt)
                .revoked(false)
                .build();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }

    public long getRemainingTtlSeconds() {
        long remaining = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, remaining);
    }
}
