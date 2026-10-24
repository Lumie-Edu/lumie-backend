package com.lumie.staff.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StaffErrorCode implements ErrorCode {

    // 400 Bad Request
    INVALID_PHONE_NUMBER("STAFF_500", "전화번호 형식이 올바르지 않습니다.", 400),
    INVALID_EMAIL_FORMAT("STAFF_501", "이메일 형식이 올바르지 않습니다.", 400),
    INVALID_ROLE("STAFF_502", "선택할 수 없는 역할입니다.", 400),

    // 404 Not Found
    STAFF_NOT_FOUND("STAFF_510", "직원을 찾을 수 없습니다.", 404),
    PERMISSION_NOT_FOUND("STAFF_512", "권한 정보를 찾을 수 없습니다.", 404),

    // 409 Conflict
    DUPLICATE_USER_LOGIN_ID("STAFF_520", "이미 사용 중인 아이디입니다.", 409),
    DUPLICATE_PERMISSION_CODE("STAFF_522", "이미 등록된 권한 코드입니다.", 409),
    STAFF_ALREADY_ACTIVE("STAFF_528", "이미 활성 상태인 직원입니다.", 409),
    DUPLICATE_PHONE("STAFF_529", "이미 사용 중인 전화번호입니다.", 409),
    CANNOT_DELETE_ACTIVE_STAFF("STAFF_533", "활성 직원은 삭제할 수 없습니다. 먼저 비활성화해 주세요.", 409),
    CANNOT_DELETE_STAFF_WITH_ASSIGNED_CLASSES("STAFF_534", "담당 수업이 있는 직원은 삭제할 수 없습니다.", 409),

    // 403 Forbidden
    QUOTA_EXCEEDED("STAFF_530", "요금제 사용 한도를 초과했습니다.", 403),
    TENANT_INVALID("STAFF_531", "학원 정보를 확인할 수 없습니다.", 403),
    INSUFFICIENT_PERMISSION("STAFF_532", "이 작업을 수행할 권한이 없습니다.", 403),

    // 502 Bad Gateway — auth-svc returned a failure result for a staff op
    AUTH_OP_FAILED("STAFF_540", "계정 정보를 처리하지 못했습니다.", 502);

    private final String code;
    private final String message;
    private final int status;
}
