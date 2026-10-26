package com.lumie.file.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FileErrorCode implements ErrorCode {

    // 400 Bad Request
    INVALID_FILE_DATA("FILE_001", "파일 정보가 올바르지 않습니다.", 400),
    INVALID_CONTENT_TYPE("FILE_002", "파일 형식이 올바르지 않습니다.", 400),
    FILE_TOO_LARGE("FILE_003", "파일 크기가 허용 범위를 초과했습니다.", 400),
    INVALID_ENTITY_TYPE("FILE_004", "항목 유형이 올바르지 않습니다.", 400),
    MAX_DEPTH_EXCEEDED("FILE_005", "폴더를 더 깊게 만들 수 없습니다.", 400),
    FOLDER_NOT_EMPTY("FILE_006", "폴더가 비어 있지 않습니다.", 400),

    // 404 Not Found
    FILE_NOT_FOUND("FILE_101", "파일을 찾을 수 없습니다.", 404),
    FOLDER_NOT_FOUND("FILE_102", "폴더를 찾을 수 없습니다.", 404),

    // 409 Conflict
    DUPLICATE_FOLDER_NAME("FILE_007", "같은 이름의 폴더가 이미 있습니다.", 409),
    FILE_IN_USE("FILE_008", "다른 자료에서 사용 중인 파일은 처리할 수 없습니다.", 409),
    UPLOAD_SESSION_INVALID("FILE_009", "업로드 세션이 만료되었거나 사용할 수 없습니다.", 409),

    // 403 Forbidden
    TENANT_INVALID("FILE_301", "학원 정보를 확인할 수 없습니다.", 403),
    ACCESS_DENIED("FILE_302", "이 파일에 접근할 권한이 없습니다.", 403),

    // 500 Internal Server Error
    STORAGE_ERROR("FILE_501", "파일을 처리하지 못했습니다.", 500),
    PRESIGNED_URL_GENERATION_FAILED("FILE_502", "파일 업로드 주소를 만들지 못했습니다.", 500);

    private final String code;
    private final String message;
    private final int status;
}
