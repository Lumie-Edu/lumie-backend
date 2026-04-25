package com.lumie.admin.application.port.out;

import java.util.Optional;

public interface AuthServicePort {
    Optional<TokenClaimsData> validateToken(String token);
    CreateUserResult createUser(CreateUserRequest request);
    DeleteUserResult deleteUser(Long userId);
    ResetPasswordResult resetPassword(Long userId);
    ChangeLoginIdResult changeLoginId(Long userId, String newLoginId);

    record TokenClaimsData(
        Long userId,
        String tenantSlug,
        Long tenantId,
        String role
    ) {}

    record CreateUserRequest(
        String name,
        String phone,
        String role,
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
