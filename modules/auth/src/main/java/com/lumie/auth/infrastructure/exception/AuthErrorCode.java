package com.lumie.auth.infrastructure.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    INVALID_CREDENTIALS("A001", "Invalid email or password", 401),
    INVALID_TOKEN("A002", "Invalid or expired token", 401),
    TOKEN_REVOKED("A003", "Token has been revoked", 401),
    USER_DISABLED("A004", "User account is disabled", 403),
    TENANT_INACTIVE("A005", "Tenant is not active", 403),
    OAUTH_PROVIDER_ERROR("A006", "OAuth provider error", 502),
    INVALID_STATE("A007", "Invalid OAuth state parameter", 400);

    private final String code;
    private final String message;
    private final int status;
}
