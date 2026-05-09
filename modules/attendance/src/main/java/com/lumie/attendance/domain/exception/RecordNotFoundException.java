package com.lumie.attendance.domain.exception;

import com.lumie.common.exception.BusinessException;

public class RecordNotFoundException extends BusinessException {

    public RecordNotFoundException(Long id) {
        super(AttendanceErrorCode.RECORD_NOT_FOUND, "Attendance record not found with id: " + id);
    }
}
