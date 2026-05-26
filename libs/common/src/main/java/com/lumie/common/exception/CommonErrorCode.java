package com.lumie.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    // 400 Bad Request
    INVALID_INPUT("COMMON_001", "Invalid input value", 400),
    INVALID_TYPE("COMMON_002", "Invalid type value", 400),
    VALIDATION_ERROR("COMMON_010", "Validation failed", 400),

    // 401 Unauthorized
    UNAUTHORIZED("COMMON_007", "Unauthorized", 401),

    // 403 Forbidden
    FORBIDDEN("COMMON_006", "Forbidden", 403),

    // 404 Not Found
    RESOURCE_NOT_FOUND("COMMON_003", "Resource not found", 404),

    // 409 Conflict
    DUPLICATE_RESOURCE("COMMON_004", "Resource already exists", 409),
    RESOURCE_CONFLICT("COMMON_008", "Resource conflict", 409),
    OPTIMISTIC_LOCK_CONFLICT("COMMON_011", "Resource modified concurrently", 409),

    // 422 Unprocessable Entity
    BUSINESS_RULE_VIOLATION("COMMON_009", "Business rule violated", 422),

    // 500 Internal Server Error
    INTERNAL_ERROR("COMMON_005", "Internal server error", 500);

    private final String code;
    private final String message;
    private final int status;
}
