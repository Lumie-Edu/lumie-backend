package com.lumie.student.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StudentErrorCode implements ErrorCode {

    // 400 Bad Request
    INVALID_PHONE_NUMBER("STUDENT_001", "Invalid phone number format", 400),
    INVALID_EMAIL_FORMAT("STUDENT_002", "Invalid email format", 400),
    INVALID_EXCEL_FILE("STUDENT_003", "Invalid Excel file format", 400),

    // 404 Not Found
    STUDENT_NOT_FOUND("STUDENT_101", "Student not found", 404),

    // 409 Conflict
    DUPLICATE_USER_LOGIN_ID("STUDENT_203", "User login ID already exists", 409),
    DUPLICATE_PHONE("STUDENT_204", "이미 사용 중인 전화번호입니다.", 409),

    // 403 Forbidden
    QUOTA_EXCEEDED("STUDENT_301", "Plan quota exceeded", 403),
    TENANT_INVALID("STUDENT_302", "Invalid tenant", 403),

    // 500 Internal — Excel parse failure on student bulk import
    EXCEL_PARSE_FAILED("STUDENT_401", "Failed to read Excel file", 500),

    // 502 Bad Gateway — auth-svc returned a failure result for a student op
    AUTH_OP_FAILED("STUDENT_402", "Auth service operation failed", 502);

    private final String code;
    private final String message;
    private final int status;
}
