package com.lumie.attendance.domain.exception;

import com.lumie.common.exception.BusinessException;

public class InvalidCodeException extends BusinessException {

    public InvalidCodeException() {
        super(AttendanceErrorCode.INVALID_CODE);
    }
}
