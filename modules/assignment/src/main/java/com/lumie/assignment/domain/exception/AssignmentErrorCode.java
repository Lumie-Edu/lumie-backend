package com.lumie.assignment.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AssignmentErrorCode implements ErrorCode {

    ASSIGNMENT_NOT_FOUND("ASSIGNMENT_001", "Assignment not found", 404),
    SUBMISSION_NOT_FOUND("ASSIGNMENT_002", "Submission not found", 404),
    DUPLICATE_SUBMISSION("ASSIGNMENT_003", "Student has already submitted for this assignment", 409),
    CLASS_NOT_FOUND("ASSIGNMENT_005", "Class not found", 404),
    CREATOR_NOT_FOUND("ASSIGNMENT_006", "Creator admin not found", 404),
    ASSIGNMENT_CLOSED("ASSIGNMENT_007", "Assignment is closed", 400),
    INVALID_STATUS("ASSIGNMENT_008", "Invalid status value", 400),
    JSON_SERIALIZATION_FAILED("ASSIGNMENT_009", "Failed to serialize or deserialize JSON field", 500),
    STUDENT_NOT_FOUND("ASSIGNMENT_010", "Student not found", 404),
    TARGET_REQUIRED("ASSIGNMENT_011", "At least one assignment target is required", 400),
    INVALID_SUBMISSION("ASSIGNMENT_012", "Invalid assignment submission", 400),
    EXAM_NOT_FOUND("ASSIGNMENT_013", "Linked exam not found", 404),
    ASSIGNMENT_NOT_VISIBLE("ASSIGNMENT_014", "Assignment is not visible to current student", 404),
    DUE_DATE_REQUIRED("ASSIGNMENT_015", "Assignment due date is required", 400);

    private final String code;
    private final String message;
    private final int status;
}
