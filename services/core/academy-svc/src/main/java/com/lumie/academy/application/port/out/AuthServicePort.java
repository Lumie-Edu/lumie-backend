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
     * Creates a new user in auth-svc.
     *
     * @param request the user creation request
     * @return the result containing success status and user ID
     */
    CreateUserResult createUser(CreateUserRequest request);

    /**
     * Deletes a user from auth-svc.
     *
     * @param userId the user ID to delete
     * @return the result containing success status
     */
    DeleteUserResult deleteUser(Long userId);

    /**
     * Token claims extracted from JWT.
     */
    record TokenClaimsData(
            Long userId,
            String tenantSlug,
            Long tenantId,
            String role
    ) {}

    /**
     * Request for creating a new user.
     */
    record CreateUserRequest(
            String userLoginId,
            String password,
            String name,
            String phone,
            String role,
            Long tenantId
    ) {}

    /**
     * Result of user creation.
     */
    record CreateUserResult(
            boolean success,
            String message,
            Long userId
    ) {}

    /**
     * Result of user deletion.
     */
    record DeleteUserResult(
            boolean success,
            String message
    ) {}
}
