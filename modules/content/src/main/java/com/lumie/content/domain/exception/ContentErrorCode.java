package com.lumie.content.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContentErrorCode implements ErrorCode {

    // 401 Unauthorized
    UNAUTHORIZED_ACCESS("CONTENT_101", "접근 권한이 없습니다.", 401),

    // 404 Not Found
    ANNOUNCEMENT_NOT_FOUND("CONTENT_201", "공지사항을 찾을 수 없습니다.", 404),
    QNA_NOT_FOUND("CONTENT_202", "질문과 답변을 찾을 수 없습니다.", 404),
    COMMENT_NOT_FOUND("CONTENT_203", "댓글을 찾을 수 없습니다.", 404),
    TEXTBOOK_NOT_FOUND("CONTENT_204", "교재를 찾을 수 없습니다.", 404),
    STUDENT_NOT_FOUND("CONTENT_207", "학생을 찾을 수 없습니다.", 404),
    STAFF_NOT_FOUND("CONTENT_208", "직원을 찾을 수 없습니다.", 404),
    REVIEW_NOT_FOUND("CONTENT_209", "후기를 찾을 수 없습니다.", 404),

    // 400 Bad Request
    INVALID_DATE_RANGE("CONTENT_403", "날짜 범위가 올바르지 않습니다.", 400),
    INVALID_CLASS_TARGET("CONTENT_405", "수업 대상이 올바르지 않습니다.", 400),
    INVALID_ANNOUNCEMENT_AUDIENCE("CONTENT_406", "공지 대상이 올바르지 않습니다.", 400);

    private final String code;
    private final String message;
    private final int status;
}
