package com.lumie.cls.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ClassErrorCode implements ErrorCode {

    CLASS_NOT_FOUND("CLASS_001", "Class not found", 404),
    ENROLLMENT_NOT_FOUND("CLASS_002", "Enrollment not found", 404),
    DUPLICATE_CLASS_NAME("CLASS_003", "Class with this name already exists in the academy", 409),
    DUPLICATE_ENROLLMENT("CLASS_004", "Student is already enrolled in this class", 409),
    CLASS_FULL("CLASS_005", "Class has reached maximum student capacity", 409),
    CLASS_CLOSED("CLASS_006", "Class is closed", 400),
    ACADEMY_NOT_FOUND("CLASS_007", "Academy not found or inactive", 404),
    TEACHER_NOT_FOUND("CLASS_008", "Teacher not found", 404),
    INVALID_STATUS("CLASS_009", "Invalid status value", 400);

    private final String code;
    private final String message;
    private final int status;
}
