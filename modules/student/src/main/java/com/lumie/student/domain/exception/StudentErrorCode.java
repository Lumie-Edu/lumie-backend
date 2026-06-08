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
    CANNOT_DELETE_ACTIVE_STUDENT("STUDENT_303", "Cannot delete active student. Deactivate first.", 409),

    // 403 Forbidden
    QUOTA_EXCEEDED("STUDENT_301", "Plan quota exceeded", 403),
    TENANT_INVALID("STUDENT_302", "Invalid tenant", 403),

    // 422 Unprocessable — bulk-import row-level validation errors
    BULK_IMPORT_NAME_REQUIRED("STUDENT_501", "이름은 필수 항목입니다.", 422),
    BULK_IMPORT_INVALID_PHONE("STUDENT_502", "전화번호 형식이 올바르지 않습니다 (예: 010-1234-5678)", 422),
    BULK_IMPORT_DUPLICATE_PHONE("STUDENT_503", "이미 등록된 전화번호입니다.", 422),
    BULK_IMPORT_DUPLICATE_STUDENT("STUDENT_504", "이미 등록된 학생입니다.", 422),
    BULK_IMPORT_QUOTA_EXCEEDED("STUDENT_505", "학생 등록 가능 인원을 초과하였습니다.", 422),
    BULK_IMPORT_AUTH_FAILED("STUDENT_506", "계정 생성에 실패하였습니다.", 422),
    BULK_IMPORT_UNEXPECTED("STUDENT_507", "예기치 못한 오류가 발생했습니다.", 500),
    BULK_IMPORT_INVALID_LENGTH("STUDENT_508", "입력값이 최대 길이를 초과했습니다.", 422),

    // 500 Internal — Excel parse failure on student bulk import
    EXCEL_PARSE_FAILED("STUDENT_401", "Failed to read Excel file", 500),

    // 502 Bad Gateway — auth-svc returned a failure result for a student op
    AUTH_OP_FAILED("STUDENT_402", "Auth service operation failed", 502);

    private final String code;
    private final String message;
    private final int status;
}
