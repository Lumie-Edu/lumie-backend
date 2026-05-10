package com.lumie.auth.api;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface AuthService {

    TokenValidationResult validateToken(String token);

    Optional<UserData> getUserInfo(String token);

    CreateUserResult createUser(String name, String phone, String role, Long tenantId);

    DeleteUserResult deleteUser(Long userId);

    ResetPasswordResult resetPassword(Long userId);

    ChangeLoginIdResult changeLoginId(Long userId, String newLoginId);

    /**
     * Batch lookup of avatar seeds keyed by user ID. Users without an
     * avatar seed are simply omitted from the returned map.
     */
    Map<Long, String> getAvatarSeeds(Collection<Long> userIds);

    record TokenValidationResult(
            boolean valid,
            String message,
            TokenClaims claims
    ) {}

    record TokenClaims(
            String sub,
            String tenantSlug,
            Long tenantId,
            String role,
            Long iat,
            Long exp,
            String jti
    ) {}

    record UserData(
            Long id,
            String email,
            String name,
            String role,
            String tenantSlug,
            Long tenantId
    ) {}

    record CreateUserResult(
            boolean success,
            String message,
            Long userId,
            String userLoginId
    ) {}

    record DeleteUserResult(
            boolean success,
            String message
    ) {}

    record ResetPasswordResult(
            boolean success,
            String message
    ) {}

    record ChangeLoginIdResult(
            boolean success,
            String message,
            String newLoginId
    ) {}
}
