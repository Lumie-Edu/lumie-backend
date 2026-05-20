package com.lumie.auth.application.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    INVALID_CREDENTIALS("AUTH_001", "Invalid email or password", 401),
    INVALID_TOKEN("AUTH_002", "Invalid or expired token", 401),
    TOKEN_REVOKED("AUTH_003", "Token has been revoked", 401),
    USER_DISABLED("AUTH_004", "User account is disabled", 403),
    TENANT_INACTIVE("AUTH_005", "Tenant is not active", 403),
    OAUTH_PROVIDER_ERROR("AUTH_006", "OAuth provider error", 502),
    INVALID_STATE("AUTH_007", "Invalid OAuth state parameter", 400),
    TOKEN_PERSISTENCE_FAILED("AUTH_008", "Failed to persist auth token", 500),
    LOGIN_ID_ALREADY_EXISTS("AUTH_009", "Login ID is already in use", 409),
    TENANT_CREATION_FAILED("AUTH_010", "Failed to provision tenant during registration", 500),
    LOGIN_ID_GENERATION_FAILED("AUTH_011", "Failed to generate a unique login ID", 500);

    private final String code;
    private final String message;
    private final int status;
}
