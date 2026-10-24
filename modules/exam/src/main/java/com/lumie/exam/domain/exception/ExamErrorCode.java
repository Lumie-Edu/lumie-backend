package com.lumie.exam.domain.exception;

import com.lumie.common.exception.ErrorCode;
import com.lumie.student.api.StudentPhoneConflict;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExamErrorCode implements ErrorCode {

    // 400 Bad Request
    INVALID_EXAM_DATA("EXAM_001", "시험 정보가 올바르지 않습니다.", 400),
    INVALID_SCORE_DATA("EXAM_002", "점수 정보가 올바르지 않습니다.", 400),
    INVALID_ANSWER_DATA("EXAM_003", "답안 정보가 올바르지 않습니다.", 400),
    INVALID_OMR_IMAGE("EXAM_004", "OMR 이미지 형식이 올바르지 않습니다.", 400),
    MISSING_TENANT_SLUG("EXAM_005", "요청에 학원 식별 정보가 없습니다.", 400),

    // 404 Not Found
    EXAM_NOT_FOUND("EXAM_101", "시험을 찾을 수 없습니다.", 404),
    RESULT_NOT_FOUND("EXAM_102", "시험 결과를 찾을 수 없습니다.", 404),
    STUDENT_NOT_FOUND("EXAM_103", "학생을 찾을 수 없습니다.", 404),
    TEMPLATE_NOT_FOUND("EXAM_104", "시험 템플릿을 찾을 수 없습니다.", 404),
    OMR_JOB_NOT_FOUND("EXAM_105", "OMR 채점 작업을 찾을 수 없습니다.", 404),
    OMR_IMAGE_NOT_FOUND("EXAM_106", "OMR 이미지를 찾을 수 없습니다.", 404),
    QUESTION_TYPE_NOT_FOUND("EXAM_107", "시험 문제 유형을 찾을 수 없습니다.", 404),
    REPORT_ZIP_EXPIRED("EXAM_108", "리포트 다운로드 기간이 만료되었습니다.", 410),

    // 409 Conflict
    DUPLICATE_RESULT("EXAM_201", "해당 학생의 시험 결과가 이미 등록되어 있습니다.", 409),
    DUPLICATE_QUESTION_TYPE("EXAM_202", "이미 등록된 시험 문제 유형입니다.", 409),
    EXAM_HAS_ACTIVE_OMR_JOB("EXAM_203", "OMR 채점 중에는 시험을 삭제할 수 없습니다.", 409),
    INACTIVE_STUDENT_PHONE(
            StudentPhoneConflict.INACTIVE_STUDENT_PHONE.code(),
            StudentPhoneConflict.INACTIVE_STUDENT_PHONE.message(),
            StudentPhoneConflict.INACTIVE_STUDENT_PHONE.status()),

    // 403 Forbidden
    OMR_QUOTA_EXCEEDED("EXAM_301", "OMR 채점 한도를 초과했습니다.", 403),
    TENANT_INVALID("EXAM_302", "학원 정보를 확인할 수 없습니다.", 403),

    // 500 Internal Server Error
    OMR_GRADING_FAILED("EXAM_501", "OMR 채점 요청을 처리하지 못했습니다.", 500),
    REPORT_GENERATION_FAILED("EXAM_502", "리포트를 생성하지 못했습니다.", 500);

    private final String code;
    private final String message;
    private final int status;
}
