package com.lumie.exam.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExamErrorCode implements ErrorCode {

    // 400 Bad Request
    INVALID_EXAM_DATA("E001", "Invalid exam data", 400),
    INVALID_SCORE_DATA("E002", "Invalid score data", 400),
    INVALID_ANSWER_DATA("E003", "Invalid answer data", 400),
    INVALID_OMR_IMAGE("E004", "Invalid OMR image", 400),

    // 404 Not Found
    EXAM_NOT_FOUND("E101", "Exam not found", 404),
    RESULT_NOT_FOUND("E102", "Exam result not found", 404),
    STUDENT_NOT_FOUND("E103", "Student not found", 404),

    // 409 Conflict
    DUPLICATE_RESULT("E201", "Exam result already exists for this student", 409),

    // 403 Forbidden
    OMR_QUOTA_EXCEEDED("E301", "OMR grading quota exceeded", 403),
    TENANT_INVALID("E302", "Invalid tenant", 403),

    // 500 Internal Server Error
    OMR_GRADING_FAILED("E501", "OMR grading service failed", 500);

    private final String code;
    private final String message;
    private final int status;
}
