package com.lumie.assignment.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AssignmentErrorCode implements ErrorCode {

    ASSIGNMENT_NOT_FOUND("ASSIGNMENT_001", "과제를 찾을 수 없습니다.", 404),
    SUBMISSION_NOT_FOUND("ASSIGNMENT_002", "제출 정보를 찾을 수 없습니다.", 404),
    DUPLICATE_SUBMISSION("ASSIGNMENT_003", "이미 제출한 과제입니다.", 409),
    CLASS_NOT_FOUND("ASSIGNMENT_005", "수업을 찾을 수 없습니다.", 404),
    CREATOR_NOT_FOUND("ASSIGNMENT_006", "과제를 등록한 직원을 찾을 수 없습니다.", 404),
    ASSIGNMENT_CLOSED("ASSIGNMENT_007", "마감된 과제입니다.", 400),
    INVALID_STATUS("ASSIGNMENT_008", "상태값이 올바르지 않습니다.", 400),
    JSON_SERIALIZATION_FAILED("ASSIGNMENT_009", "요청 정보를 처리하지 못했습니다.", 500),
    STUDENT_NOT_FOUND("ASSIGNMENT_010", "학생을 찾을 수 없습니다.", 404),
    TARGET_REQUIRED("ASSIGNMENT_011", "과제 대상을 한 명 이상 선택해 주세요.", 400),
    INVALID_SUBMISSION("ASSIGNMENT_012", "과제 제출 정보가 올바르지 않습니다.", 400),
    EXAM_NOT_FOUND("ASSIGNMENT_013", "연결된 시험을 찾을 수 없습니다.", 404),
    ASSIGNMENT_NOT_VISIBLE("ASSIGNMENT_014", "현재 학생에게 공개되지 않은 과제입니다.", 404),
    DUE_DATE_REQUIRED("ASSIGNMENT_015", "과제 마감일을 입력해 주세요.", 400),
    TARGET_IMMUTABLE("ASSIGNMENT_016", "과제 등록 후에는 배정 대상을 변경할 수 없습니다.", 409);

    private final String code;
    private final String message;
    private final int status;
}
