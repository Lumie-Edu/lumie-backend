package com.lumie.content.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContentErrorCode implements ErrorCode {

    // 401 Unauthorized
    UNAUTHORIZED_ACCESS("CONTENT_101", "Unauthorized access", 401),

    // 404 Not Found
    ANNOUNCEMENT_NOT_FOUND("CONTENT_201", "Announcement not found", 404),
    QNA_NOT_FOUND("CONTENT_202", "Q&A not found", 404),
    COMMENT_NOT_FOUND("CONTENT_203", "Comment not found", 404),
    TEXTBOOK_NOT_FOUND("CONTENT_204", "Textbook not found", 404),
    STUDENT_NOT_FOUND("CONTENT_207", "Student not found", 404),
    ADMIN_NOT_FOUND("CONTENT_208", "Admin not found", 404),
    REVIEW_NOT_FOUND("CONTENT_209", "Review not found", 404),

    // 400 Bad Request
    INVALID_DATE_RANGE("CONTENT_403", "Invalid date range", 400),
    INVALID_CLASS_TARGET("CONTENT_405", "Invalid class target", 400);

    private final String code;
    private final String message;
    private final int status;
}
