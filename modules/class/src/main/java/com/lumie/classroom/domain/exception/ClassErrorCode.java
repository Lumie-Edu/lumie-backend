package com.lumie.classroom.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ClassErrorCode implements ErrorCode {

    CLASS_NOT_FOUND("CLASS_001", "Class not found", 404),
    ENROLLMENT_NOT_FOUND("CLASS_002", "Enrollment not found", 404),
    DUPLICATE_ENROLLMENT("CLASS_004", "Student is already enrolled in this class", 409),
    TEACHER_NOT_FOUND("CLASS_008", "Teacher not found", 404),
    INVALID_STATUS("CLASS_009", "Invalid status value", 400),
    SCHEDULE_OVERLAP("CLASS_010", "Teacher already has a class at the overlapping time", 409),
    STUDENT_SCHEDULE_OVERLAP("CLASS_011", "Student already has a class at the overlapping time", 409);

    private final String code;
    private final String message;
    private final int status;
}
