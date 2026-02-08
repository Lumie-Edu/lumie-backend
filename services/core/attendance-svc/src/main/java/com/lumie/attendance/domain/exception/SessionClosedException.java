package com.lumie.attendance.domain.exception;

import com.lumie.common.exception.BusinessException;

public class SessionClosedException extends BusinessException {

    public SessionClosedException(Long id) {
        super(AttendanceErrorCode.SESSION_CLOSED, "Attendance session is closed: " + id);
    }
}
