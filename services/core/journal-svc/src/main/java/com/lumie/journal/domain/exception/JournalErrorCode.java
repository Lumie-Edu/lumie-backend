package com.lumie.journal.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JournalErrorCode implements ErrorCode {

    JOURNAL_NOT_FOUND("JOURNAL_001", "Journal not found", 404),
    CLASS_NOT_FOUND("JOURNAL_002", "Class not found", 404),
    ACADEMY_NOT_FOUND("JOURNAL_003", "Academy not found or inactive", 404),
    TEACHER_NOT_FOUND("JOURNAL_004", "Teacher not found", 404),
    INVALID_STATUS("JOURNAL_005", "Invalid status value", 400),
    DUPLICATE_JOURNAL_DATE("JOURNAL_006", "A journal already exists for this class on the given date", 409);

    private final String code;
    private final String message;
    private final int status;
}
