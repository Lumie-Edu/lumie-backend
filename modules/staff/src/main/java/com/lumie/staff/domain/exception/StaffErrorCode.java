package com.lumie.staff.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StaffErrorCode implements ErrorCode {

    // 400 Bad Request
    INVALID_PHONE_NUMBER("STAFF_500", "Invalid phone number format", 400),
    INVALID_EMAIL_FORMAT("STAFF_501", "Invalid email format", 400),
    INVALID_ROLE("STAFF_502", "Invalid or non-creatable role", 400),

    // 404 Not Found
    STAFF_NOT_FOUND("STAFF_510", "Staff not found", 404),
    PERMISSION_NOT_FOUND("STAFF_512", "Permission not found", 404),

    // 409 Conflict
    DUPLICATE_USER_LOGIN_ID("STAFF_520", "User login ID already exists", 409),
    DUPLICATE_PERMISSION_CODE("STAFF_522", "Permission code already exists", 409),
    STAFF_ALREADY_ACTIVE("STAFF_528", "Staff is already active", 409),
    DUPLICATE_PHONE("STAFF_529", "이미 사용 중인 전화번호입니다.", 409),
    CANNOT_DELETE_ACTIVE_STAFF("STAFF_533", "Cannot delete active staff. Deactivate first.", 409),
    CANNOT_DELETE_STAFF_WITH_ASSIGNED_CLASSES("STAFF_534", "Cannot delete staff with assigned classes.", 409),

    // 403 Forbidden
    QUOTA_EXCEEDED("STAFF_530", "Plan quota exceeded", 403),
    TENANT_INVALID("STAFF_531", "Invalid tenant", 403),
    INSUFFICIENT_PERMISSION("STAFF_532", "Insufficient permission", 403),

    // 502 Bad Gateway — auth-svc returned a failure result for a staff op
    AUTH_OP_FAILED("STAFF_540", "Auth service operation failed", 502);

    private final String code;
    private final String message;
    private final int status;
}
