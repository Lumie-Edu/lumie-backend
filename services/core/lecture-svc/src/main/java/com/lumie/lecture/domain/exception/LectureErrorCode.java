package com.lumie.lecture.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LectureErrorCode implements ErrorCode {

    LECTURE_NOT_FOUND(404, "LECTURE_001", "강의를 찾을 수 없습니다"),
    INVALID_FILE_TYPE(400, "LECTURE_002", "지원하지 않는 파일 형식입니다"),
    FILE_TOO_LARGE(400, "LECTURE_003", "파일 크기가 제한을 초과합니다"),
    UPLOAD_NOT_COMPLETED(400, "LECTURE_004", "업로드가 완료되지 않았습니다"),
    PROCESSING_IN_PROGRESS(409, "LECTURE_005", "처리가 진행 중입니다"),
    STORAGE_ERROR(500, "LECTURE_006", "스토리지 오류가 발생했습니다"),
    UPLOAD_INITIATION_FAILED(500, "LECTURE_007", "업로드 시작에 실패했습니다"),
    PRESIGNED_URL_FAILED(500, "LECTURE_008", "Presigned URL 생성에 실패했습니다"),
    UPLOAD_COMPLETION_FAILED(500, "LECTURE_009", "업로드 완료 처리에 실패했습니다");

    private final int status;
    private final String code;
    private final String message;
}
