package com.lumie.academy.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AcademyErrorCode implements ErrorCode {

    // 400 Bad Request
    INVALID_PHONE_NUMBER("A001", "Invalid phone number format", 400),
    INVALID_EMAIL_FORMAT("A002", "Invalid email format", 400),
    INVALID_EXCEL_FILE("A003", "Invalid Excel file format", 400),

    // 404 Not Found
    STUDENT_NOT_FOUND("A101", "Student not found", 404),
    ADMIN_NOT_FOUND("A102", "Admin not found", 404),
    ACADEMY_NOT_FOUND("A103", "Academy not found", 404),
    POSITION_NOT_FOUND("A104", "Position not found", 404),

    // 409 Conflict
    DUPLICATE_EMAIL("A201", "Email already exists", 409),
    DUPLICATE_USER_LOGIN_ID("A203", "User login ID already exists", 409),
    DUPLICATE_ACADEMY_NAME("A202", "Academy name already exists", 409),
    DUPLICATE_POSITION_NAME("A204", "Position name already exists", 409),
    POSITION_IN_USE("A205", "해당 직책을 사용 중인 직원이 있어 삭제할 수 없습니다.", 409),

    // 403 Forbidden
    QUOTA_EXCEEDED("A301", "Plan quota exceeded", 403),
    TENANT_INVALID("A302", "Invalid tenant", 403);

    private final String code;
    private final String message;
    private final int status;
}
