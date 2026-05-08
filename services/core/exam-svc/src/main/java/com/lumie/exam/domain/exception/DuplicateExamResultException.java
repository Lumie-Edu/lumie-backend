package com.lumie.exam.domain.exception;

/**
 * Thrown when persisting an {@code ExamResult} would violate the
 * {@code (exam_id, student_id)} uniqueness invariant. Wraps the Spring
 * {@code DataIntegrityViolationException} so the application layer doesn't
 * have to import infrastructure exception types.
 */
public class DuplicateExamResultException extends ExamException {
    public DuplicateExamResultException() {
        super(ExamErrorCode.INVALID_EXAM_DATA, "Duplicate exam result");
    }

    public DuplicateExamResultException(Throwable cause) {
        super(ExamErrorCode.INVALID_EXAM_DATA, "Duplicate exam result");
        initCause(cause);
    }
}
