package com.lumie.attendance.domain.exception;

import com.lumie.common.exception.BusinessException;

public class SessionNotFoundException extends BusinessException {

    public SessionNotFoundException(Long id) {
        super(AttendanceErrorCode.SESSION_NOT_FOUND, "Attendance session not found with id: " + id);
    }
}
