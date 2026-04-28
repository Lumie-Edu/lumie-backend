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
 * @param jti unique identifier for this individual token
 * @param sid session identifier shared by both access and refresh tokens of the same login
 */
public record TokenClaims(
        String sub,
        String tenantSlug,
        Long tenantId,
        Role role,
        Instant iat,
        Instant exp,
        String jti,
        String sid
) {
    public static TokenClaims of(String sub, String tenantSlug, Long tenantId,
                                  Role role, Instant iat, Instant exp, String jti, String sid) {
        return new TokenClaims(sub, tenantSlug, tenantId, role, iat, exp, jti, sid);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(exp);
    }

    public long getUserId() {
        return Long.parseLong(sub);
    }
}
