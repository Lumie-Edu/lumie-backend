package com.lumie.admin.domain.exception;

import com.lumie.common.exception.BusinessException;

public class AttendanceNotFoundException extends BusinessException {
    public AttendanceNotFoundException(Long id) {
        super(AdminErrorCode.ATTENDANCE_NOT_FOUND,
              String.format("Attendance not found with id: %d", id));
    }
}
