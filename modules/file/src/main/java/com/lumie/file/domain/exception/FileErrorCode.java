package com.lumie.file.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FileErrorCode implements ErrorCode {

    // 400 Bad Request
    INVALID_FILE_DATA("FILE_001", "Invalid file data", 400),
    INVALID_CONTENT_TYPE("FILE_002", "Invalid content type", 400),
    FILE_TOO_LARGE("FILE_003", "File exceeds maximum size", 400),
    INVALID_ENTITY_TYPE("FILE_004", "Invalid entity type", 400),
    MAX_DEPTH_EXCEEDED("FILE_005", "Maximum folder depth exceeded", 400),
    FOLDER_NOT_EMPTY("FILE_006", "Folder is not empty", 400),

    // 404 Not Found
    FILE_NOT_FOUND("FILE_101", "File not found", 404),
    FOLDER_NOT_FOUND("FILE_102", "Folder not found", 404),

    // 409 Conflict
    DUPLICATE_FOLDER_NAME("FILE_007", "Folder with this name already exists", 409),
    FILE_IN_USE("FILE_008", "File is linked to another resource", 409),

    // 403 Forbidden
    TENANT_INVALID("FILE_301", "Invalid tenant", 403),
    ACCESS_DENIED("FILE_302", "Access denied to this file", 403),

    // 500 Internal Server Error
    STORAGE_ERROR("FILE_501", "Storage operation failed", 500),
    PRESIGNED_URL_GENERATION_FAILED("FILE_502", "Failed to generate presigned URL", 500);

    private final String code;
    private final String message;
    private final int status;
}
