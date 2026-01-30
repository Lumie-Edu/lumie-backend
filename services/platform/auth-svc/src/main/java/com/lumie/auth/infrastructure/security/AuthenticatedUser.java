package com.lumie.auth.infrastructure.security;

import com.lumie.auth.domain.vo.Role;

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
