package com.lumie.lecture.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LectureErrorCode implements ErrorCode {

    LECTURE_NOT_FOUND(404, "LECTURE_001", "강의를 찾을 수 없습니다"),
    STORAGE_ERROR(500, "LECTURE_006", "스토리지 오류가 발생했습니다"),
    PRESIGNED_URL_FAILED(500, "LECTURE_008", "Presigned URL 생성에 실패했습니다");

    private final int status;
    private final String code;
    private final String message;
}
