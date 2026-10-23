package com.lumie.exam.domain.exception;

import com.lumie.common.exception.ErrorCode;
import com.lumie.student.api.StudentPhoneConflict;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExamErrorCode implements ErrorCode {

    // 400 Bad Request
    INVALID_EXAM_DATA("EXAM_001", "Invalid exam data", 400),
    INVALID_SCORE_DATA("EXAM_002", "Invalid score data", 400),
    INVALID_ANSWER_DATA("EXAM_003", "Invalid answer data", 400),
    INVALID_OMR_IMAGE("EXAM_004", "Invalid OMR image", 400),
    MISSING_TENANT_SLUG("EXAM_005", "Callback payload missing tenant slug", 400),

    // 404 Not Found
    EXAM_NOT_FOUND("EXAM_101", "Exam not found", 404),
    RESULT_NOT_FOUND("EXAM_102", "Exam result not found", 404),
    STUDENT_NOT_FOUND("EXAM_103", "Student not found", 404),
    TEMPLATE_NOT_FOUND("EXAM_104", "Exam template not found", 404),
    OMR_JOB_NOT_FOUND("EXAM_105", "OMR grading job not found", 404),
    OMR_IMAGE_NOT_FOUND("EXAM_106", "OMR image not found", 404),
    QUESTION_TYPE_NOT_FOUND("EXAM_107", "Exam question type not found", 404),
    REPORT_ZIP_EXPIRED("EXAM_108", "Report ZIP has expired", 410),

    // 409 Conflict
    DUPLICATE_RESULT("EXAM_201", "Exam result already exists for this student", 409),
    DUPLICATE_QUESTION_TYPE("EXAM_202", "Exam question type already exists", 409),
    INACTIVE_STUDENT_PHONE(
            StudentPhoneConflict.INACTIVE_STUDENT_PHONE.code(),
            StudentPhoneConflict.INACTIVE_STUDENT_PHONE.message(),
            StudentPhoneConflict.INACTIVE_STUDENT_PHONE.status()),

    // 403 Forbidden
    OMR_QUOTA_EXCEEDED("EXAM_301", "OMR grading quota exceeded", 403),
    TENANT_INVALID("EXAM_302", "Invalid tenant", 403),

    // 500 Internal Server Error
    OMR_GRADING_FAILED("EXAM_501", "OMR grading service failed", 500),
    REPORT_GENERATION_FAILED("EXAM_502", "Report generation service failed", 500);

    private final String code;
    private final String message;
    private final int status;
}
