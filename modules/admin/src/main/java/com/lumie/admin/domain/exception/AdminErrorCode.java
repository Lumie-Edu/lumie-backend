package com.lumie.admin.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AdminErrorCode implements ErrorCode {

    // 400 Bad Request
    INVALID_PHONE_NUMBER("ADMIN_500", "Invalid phone number format", 400),
    INVALID_EMAIL_FORMAT("ADMIN_501", "Invalid email format", 400),

    // 404 Not Found
    ADMIN_NOT_FOUND("ADMIN_510", "Admin not found", 404),
    PERMISSION_NOT_FOUND("ADMIN_512", "Permission not found", 404),

    // 409 Conflict
    DUPLICATE_USER_LOGIN_ID("ADMIN_520", "User login ID already exists", 409),
    DUPLICATE_PERMISSION_CODE("ADMIN_522", "Permission code already exists", 409),
    ADMIN_ALREADY_ACTIVE("ADMIN_528", "Admin is already active", 409),
    DUPLICATE_PHONE("ADMIN_529", "이미 사용 중인 전화번호입니다.", 409),

    // 403 Forbidden
    QUOTA_EXCEEDED("ADMIN_530", "Plan quota exceeded", 403),
    TENANT_INVALID("ADMIN_531", "Invalid tenant", 403),
    INSUFFICIENT_PERMISSION("ADMIN_532", "Insufficient permission", 403),

    // 502 Bad Gateway — auth-svc returned a failure result for an admin op
    AUTH_OP_FAILED("ADMIN_540", "Auth service operation failed", 502);

    private final String code;
    private final String message;
    private final int status;
}
