package com.lumie.auth.application.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    INVALID_CREDENTIALS("AUTH_001", "Invalid email or password", 401),
    USER_DISABLED("AUTH_004", "User account is disabled", 403),
    TENANT_INACTIVE("AUTH_005", "Tenant is not active", 403),
    SESSION_PERSISTENCE_FAILED("AUTH_008", "Failed to persist auth session", 500),
    LOGIN_ID_ALREADY_EXISTS("AUTH_009", "Login ID is already in use", 409),
    TENANT_CREATION_FAILED("AUTH_010", "Failed to provision tenant during registration", 500),
    LOGIN_ID_GENERATION_FAILED("AUTH_011", "Failed to generate a unique login ID", 500),
    CURRENT_PASSWORD_INCORRECT("AUTH_015", "Current password is incorrect", 422),
    PASSWORD_UNCHANGED("AUTH_016", "New password must differ from the current password", 422);

    private final String code;
    private final String message;
    private final int status;
}
