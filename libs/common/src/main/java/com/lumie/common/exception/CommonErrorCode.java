package com.lumie.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    // 400 Bad Request
    INVALID_INPUT("COMMON_001", "입력값이 올바르지 않습니다.", 400),
    INVALID_TYPE("COMMON_002", "유형값이 올바르지 않습니다.", 400),
    VALIDATION_ERROR("COMMON_010", "입력값을 확인해 주세요.", 400),
    MISSING_IDEMPOTENCY_KEY("COMMON_012", "요청 식별 정보가 필요합니다.", 400),
    INVALID_IDEMPOTENCY_KEY_FORMAT("COMMON_013", "요청 식별 정보 형식이 올바르지 않습니다.", 400),

    // 401 Unauthorized
    UNAUTHORIZED("COMMON_007", "로그인이 필요합니다. 다시 로그인해 주세요.", 401),

    // 403 Forbidden
    FORBIDDEN("COMMON_006", "이 작업을 수행할 권한이 없습니다.", 403),

    // 404 Not Found
    RESOURCE_NOT_FOUND("COMMON_003", "자료를 찾을 수 없습니다.", 404),

    // 409 Conflict
    DUPLICATE_RESOURCE("COMMON_004", "이미 등록된 자료입니다.", 409),
    RESOURCE_CONFLICT("COMMON_008", "요청한 자료가 현재 상태와 충돌합니다.", 409),
    OPTIMISTIC_LOCK_CONFLICT("COMMON_011", "다른 곳에서 항목이 수정되었습니다. 새로고침 후 다시 시도해 주세요.", 409),

    // 422 Unprocessable Entity
    BUSINESS_RULE_VIOLATION("COMMON_009", "현재 조건에서는 요청을 처리할 수 없습니다.", 422),

    // 500 Internal Server Error
    INTERNAL_ERROR("COMMON_005", "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.", 500);

    private final String code;
    private final String message;
    private final int status;
}
