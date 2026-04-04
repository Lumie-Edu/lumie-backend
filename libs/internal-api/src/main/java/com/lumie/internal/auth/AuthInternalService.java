package com.lumie.internal.auth;

import java.util.Optional;

public interface AuthInternalService {

    TokenValidationResult validateToken(String token);

    Optional<UserData> getUserInfo(String token);

    CreateUserResult createUser(String userLoginId, String password, String name,
                                String phone, String role, Long tenantId);

    DeleteUserResult deleteUser(Long userId);

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
            Long userId
    ) {}

    record DeleteUserResult(
            boolean success,
            String message
    ) {}
}
