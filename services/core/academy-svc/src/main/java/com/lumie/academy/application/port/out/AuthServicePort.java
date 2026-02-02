package com.lumie.academy.application.port.out;

import java.util.Optional;

/**
 * Output port for auth-svc gRPC client.
 * Used to validate JWT tokens and extract user information.
 */
public interface AuthServicePort {

    /**
     * Validates a JWT token and returns the claims if valid.
     *
     * @param token the JWT token
     * @return optional containing token claims if valid
     */
    Optional<TokenClaimsData> validateToken(String token);

    /**
     * Token claims extracted from JWT.
     */
    record TokenClaimsData(
            Long userId,
            String tenantSlug,
            Long tenantId,
            String role
    ) {}
}
