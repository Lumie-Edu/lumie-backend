package com.lumie.auth.domain.vo;

import java.time.Instant;

/**
 * Immutable value object representing JWT claims.
 *
 * @param sub the subject (user ID)
 * @param tenantSlug the tenant slug
 * @param tenantId the tenant ID
 * @param role the user role
 * @param iat issued at timestamp
 * @param exp expiration timestamp
 * @param jti JWT ID (unique identifier)
 */
public record TokenClaims(
        String sub,
        String tenantSlug,
        Long tenantId,
        Role role,
        Instant iat,
        Instant exp,
        String jti
) {
    public static TokenClaims of(String sub, String tenantSlug, Long tenantId,
                                  Role role, Instant iat, Instant exp, String jti) {
        return new TokenClaims(sub, tenantSlug, tenantId, role, iat, exp, jti);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(exp);
    }

    public long getUserId() {
        return Long.parseLong(sub);
    }
}
