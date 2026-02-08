package com.lumie.attendance.domain.exception;

import com.lumie.common.exception.BusinessException;

public class DuplicateRecordException extends BusinessException {

    public DuplicateRecordException(Long sessionId, Long studentId) {
        super(AttendanceErrorCode.DUPLICATE_RECORD,
              "Attendance record already exists for session " + sessionId + " and student " + studentId);
    }
}
