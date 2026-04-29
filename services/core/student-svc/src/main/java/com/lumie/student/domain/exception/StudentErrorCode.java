package com.lumie.student.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StudentErrorCode implements ErrorCode {

    // 400 Bad Request
    INVALID_PHONE_NUMBER("S001", "Invalid phone number format", 400),
    INVALID_EMAIL_FORMAT("S002", "Invalid email format", 400),
    INVALID_EXCEL_FILE("S003", "Invalid Excel file format", 400),

    // 404 Not Found
    STUDENT_NOT_FOUND("S101", "Student not found", 404),

    // 409 Conflict
    DUPLICATE_USER_LOGIN_ID("S203", "User login ID already exists", 409),
    DUPLICATE_PHONE("S204", "이미 사용 중인 전화번호입니다.", 409),

    // 403 Forbidden
    QUOTA_EXCEEDED("S301", "Plan quota exceeded", 403),
    TENANT_INVALID("S302", "Invalid tenant", 403);

    private final String code;
    private final String message;
    private final int status;
}
