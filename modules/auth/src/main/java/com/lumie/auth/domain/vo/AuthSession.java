package com.lumie.auth.domain.vo;

import com.lumie.common.auth.Role;

import java.time.Instant;

/**
 * The server-side authentication state addressed by an opaque browser cookie.
 * The cookie contains only {@link #id()}; all identity and tenant data stays in Redis.
 */
public record AuthSession(
        String id,
        Long userId,
        String userName,
        String tenantSlug,
        Long tenantId,
        Role role,
        String device,
        DeviceCategory deviceCategory,
        Instant createdAt,
        Instant expiresAt
) {
    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }
}
