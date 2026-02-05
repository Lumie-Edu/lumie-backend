package com.lumie.spreadsheet.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SpreadsheetErrorCode implements ErrorCode {

    // 400 Bad Request
    INVALID_CELL_ADDRESS("S001", "Invalid cell address", 400),
    INVALID_CELL_DATA("S002", "Invalid cell data", 400),
    INVALID_SPREADSHEET_DATA("S003", "Invalid spreadsheet data", 400),

    // 403 Forbidden
    ACCESS_DENIED("S101", "Access denied to this spreadsheet", 403),
    EDIT_NOT_ALLOWED("S102", "Edit not allowed for this spreadsheet", 403),
    CELL_LOCKED("S103", "Cell is locked by another user", 403),

    // 404 Not Found
    SPREADSHEET_NOT_FOUND("S201", "Spreadsheet not found", 404),

    // 409 Conflict
    CELL_LOCK_CONFLICT("S301", "Cell lock conflict", 409),
    CONCURRENT_EDIT_CONFLICT("S302", "Concurrent edit conflict", 409);

    private final String code;
    private final String message;
    private final int status;
}
