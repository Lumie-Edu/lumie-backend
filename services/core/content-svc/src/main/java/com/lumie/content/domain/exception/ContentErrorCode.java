package com.lumie.content.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContentErrorCode implements ErrorCode {

    // 401 Unauthorized
    UNAUTHORIZED_ACCESS("C101", "Unauthorized access", 401),

    // 404 Not Found
    ANNOUNCEMENT_NOT_FOUND("C201", "Announcement not found", 404),
    QNA_NOT_FOUND("C202", "Q&A not found", 404),
    COMMENT_NOT_FOUND("C203", "Comment not found", 404),
    TEXTBOOK_NOT_FOUND("C204", "Textbook not found", 404),
    SCHEDULE_NOT_FOUND("C205", "Schedule not found", 404),
    RESERVATION_NOT_FOUND("C206", "Reservation not found", 404),
    STUDENT_NOT_FOUND("C207", "Student not found", 404),
    ADMIN_NOT_FOUND("C208", "Admin not found", 404),
    REVIEW_NOT_FOUND("C209", "Review not found", 404),

    // 409 Conflict
    DUPLICATE_RESERVATION("C301", "Duplicate reservation", 409),
    SCHEDULE_FULL("C302", "Schedule is full", 409),

    // 400 Bad Request
    INVALID_RESERVATION_STATUS("C401", "Invalid reservation status transition", 400),
    SCHEDULE_NOT_AVAILABLE("C402", "Schedule is not available", 400),
    INVALID_DATE_RANGE("C403", "Invalid date range", 400);

    private final String code;
    private final String message;
    private final int status;
}
