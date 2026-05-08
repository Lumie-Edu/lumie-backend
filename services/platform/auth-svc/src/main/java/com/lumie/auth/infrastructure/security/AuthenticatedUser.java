package com.lumie.auth.infrastructure.security;

import com.lumie.common.auth.Role;

/**
 * Represents an authenticated user in the security context.
 */
public record AuthenticatedUser(
        Long userId,
        String tenantSlug,
        Long tenantId,
        Role role
) {
}
