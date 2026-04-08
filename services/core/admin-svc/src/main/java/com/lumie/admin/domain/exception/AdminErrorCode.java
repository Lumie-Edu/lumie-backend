package com.lumie.admin.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AdminErrorCode implements ErrorCode {

    // 400 Bad Request
    INVALID_PHONE_NUMBER("A500", "Invalid phone number format", 400),
    INVALID_EMAIL_FORMAT("A501", "Invalid email format", 400),

    // 404 Not Found
    ADMIN_NOT_FOUND("A510", "Admin not found", 404),
    PERMISSION_NOT_FOUND("A512", "Permission not found", 404),
    ACADEMY_NOT_FOUND("A518", "Academy not found", 404),

    // 409 Conflict
    DUPLICATE_USER_LOGIN_ID("A520", "User login ID already exists", 409),
    DUPLICATE_PERMISSION_CODE("A522", "Permission code already exists", 409),
    ADMIN_ALREADY_ACTIVE("A528", "Admin is already active", 409),
    DUPLICATE_PHONE("A529", "이미 사용 중인 전화번호입니다.", 409),

    // 403 Forbidden
    QUOTA_EXCEEDED("A530", "Plan quota exceeded", 403),
    TENANT_INVALID("A531", "Invalid tenant", 403),
    INSUFFICIENT_PERMISSION("A532", "Insufficient permission", 403);

    private final String code;
    private final String message;
    private final int status;
}
