package com.lumie.classroom.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ClassErrorCode implements ErrorCode {

    CLASS_NOT_FOUND("CLASS_001", "수업을 찾을 수 없습니다.", 404),
    ENROLLMENT_NOT_FOUND("CLASS_002", "수강 정보를 찾을 수 없습니다.", 404),
    DUPLICATE_ENROLLMENT("CLASS_004", "이미 수강 중인 학생입니다.", 409),
    TEACHER_NOT_FOUND("CLASS_008", "담당 강사를 찾을 수 없습니다.", 404),
    INVALID_STATUS("CLASS_009", "상태값이 올바르지 않습니다.", 400),
    SCHEDULE_OVERLAP("CLASS_010", "강사의 수업 시간이 다른 수업과 겹칩니다.", 409),
    STUDENT_SCHEDULE_OVERLAP("CLASS_011", "학생의 수업 시간이 다른 수업과 겹칩니다.", 409),
    JSON_SERIALIZATION_FAILED("CLASS_012", "요청 정보를 처리하지 못했습니다.", 500);

    private final String code;
    private final String message;
    private final int status;
}
