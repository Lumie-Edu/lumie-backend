package com.lumie.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    // 400 Bad Request
    INVALID_INPUT("C001", "Invalid input value", 400),
    INVALID_TYPE("C002", "Invalid type value", 400),

    // 403 Forbidden
    FORBIDDEN("C006", "Forbidden", 403),

    // 404 Not Found
    RESOURCE_NOT_FOUND("C003", "Resource not found", 404),

    // 409 Conflict
    DUPLICATE_RESOURCE("C004", "Resource already exists", 409),

    // 500 Internal Server Error
    INTERNAL_ERROR("C005", "Internal server error", 500);

    private final String code;
    private final String message;
    private final int status;
}
