package com.lumie.spreadsheet.domain.exception;

import com.lumie.common.exception.BusinessException;

public class SpreadsheetException extends BusinessException {

    public SpreadsheetException(SpreadsheetErrorCode errorCode) {
        super(errorCode);
    }

    public SpreadsheetException(SpreadsheetErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
