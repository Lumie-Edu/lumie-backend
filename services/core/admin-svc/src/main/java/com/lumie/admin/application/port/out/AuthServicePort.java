package com.lumie.admin.application.port.out;

import java.util.Optional;

public interface AuthServicePort {
    Optional<TokenClaimsData> validateToken(String token);
    CreateUserResult createUser(CreateUserRequest request);
    DeleteUserResult deleteUser(Long userId);

    record TokenClaimsData(
        Long userId,
        String tenantSlug,
        Long tenantId,
        String role
    ) {}

    record CreateUserRequest(
        String userLoginId,
        String password,
        String name,
        String phone,
        String role,
        Long tenantId
    ) {}

    record CreateUserResult(
        boolean success,
        String message,
        Long userId
    ) {}

    record DeleteUserResult(
        boolean success,
        String message
    ) {}
}
