package com.lumie.file.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FileErrorCode implements ErrorCode {

    // 400 Bad Request
    INVALID_FILE_DATA("F001", "Invalid file data", 400),
    INVALID_CONTENT_TYPE("F002", "Invalid content type", 400),
    FILE_TOO_LARGE("F003", "File exceeds maximum size", 400),
    INVALID_ENTITY_TYPE("F004", "Invalid entity type", 400),

    // 404 Not Found
    FILE_NOT_FOUND("F101", "File not found", 404),

    // 403 Forbidden
    TENANT_INVALID("F301", "Invalid tenant", 403),
    ACCESS_DENIED("F302", "Access denied to this file", 403),

    // 500 Internal Server Error
    STORAGE_ERROR("F501", "Storage operation failed", 500),
    PRESIGNED_URL_GENERATION_FAILED("F502", "Failed to generate presigned URL", 500);

    private final String code;
    private final String message;
    private final int status;
}
