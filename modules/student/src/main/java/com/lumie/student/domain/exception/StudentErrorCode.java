package com.lumie.student.domain.exception;

import com.lumie.common.exception.ErrorCode;
import com.lumie.student.api.StudentPhoneConflict;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StudentErrorCode implements ErrorCode {

    // 400 Bad Request
    INVALID_PHONE_NUMBER("STUDENT_001", "전화번호 형식이 올바르지 않습니다.", 400),
    INVALID_EMAIL_FORMAT("STUDENT_002", "이메일 형식이 올바르지 않습니다.", 400),
    INVALID_EXCEL_FILE("STUDENT_003", "엑셀 파일 형식이 올바르지 않습니다.", 400),

    // 404 Not Found
    STUDENT_NOT_FOUND("STUDENT_101", "학생을 찾을 수 없습니다.", 404),

    // 409 Conflict
    DUPLICATE_USER_LOGIN_ID("STUDENT_203", "이미 사용 중인 아이디입니다.", 409),
    DUPLICATE_PHONE("STUDENT_204", "이미 사용 중인 전화번호입니다.", 409),
    INACTIVE_STUDENT_PHONE(
            StudentPhoneConflict.INACTIVE_STUDENT_PHONE.code(),
            StudentPhoneConflict.INACTIVE_STUDENT_PHONE.message(),
            StudentPhoneConflict.INACTIVE_STUDENT_PHONE.status()),
    CANNOT_DELETE_ACTIVE_STUDENT("STUDENT_303", "재원 중인 학생은 삭제할 수 없습니다. 먼저 퇴원 처리해 주세요.", 409),
    CANNOT_DELETE_STUDENT_WITH_ACTIVE_ENROLLMENT("STUDENT_304", "수강 중인 수업이 있는 학생은 삭제할 수 없습니다.", 409),

    // 403 Forbidden
    QUOTA_EXCEEDED("STUDENT_301", "요금제 사용 한도를 초과했습니다.", 403),
    TENANT_INVALID("STUDENT_302", "학원 정보를 확인할 수 없습니다.", 403),

    // 422 Unprocessable — bulk-import row-level validation errors
    BULK_IMPORT_NAME_REQUIRED("STUDENT_501", "이름은 필수 항목입니다.", 422),
    BULK_IMPORT_INVALID_PHONE("STUDENT_502", "전화번호 형식이 올바르지 않습니다 (예: 010-1234-5678)", 422),
    BULK_IMPORT_DUPLICATE_PHONE("STUDENT_503", "이미 등록된 전화번호입니다.", 422),
    BULK_IMPORT_INACTIVE_STUDENT_PHONE(
            "STUDENT_510", StudentPhoneConflict.INACTIVE_STUDENT_PHONE.message(), 422),
    BULK_IMPORT_DUPLICATE_STUDENT("STUDENT_504", "이미 등록된 학생입니다.", 422),
    BULK_IMPORT_QUOTA_EXCEEDED("STUDENT_505", "학생 등록 가능 인원을 초과하였습니다.", 422),
    BULK_IMPORT_AUTH_FAILED("STUDENT_506", "계정 생성에 실패하였습니다.", 422),
    BULK_IMPORT_UNEXPECTED("STUDENT_507", "예기치 못한 오류가 발생했습니다.", 500),
    BULK_IMPORT_INVALID_LENGTH("STUDENT_508", "입력값이 최대 길이를 초과했습니다.", 422),
    BULK_IMPORT_UNSAFE_SPREADSHEET_VALUE("STUDENT_509", "스프레드시트 수식으로 해석될 수 있는 값은 입력할 수 없습니다.", 422),

    // 500 Internal — Excel parse failure on student bulk import
    EXCEL_PARSE_FAILED("STUDENT_401", "엑셀 파일을 읽지 못했습니다.", 500),

    // 502 Bad Gateway — auth-svc returned a failure result for a student op
    AUTH_OP_FAILED("STUDENT_402", "계정 정보를 처리하지 못했습니다.", 502);

    private final String code;
    private final String message;
    private final int status;
}
