package com.lumie.attendance.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AttendanceErrorCode implements ErrorCode {

    SESSION_NOT_FOUND("ATTENDANCE_001", "Attendance session not found", 404),
    RECORD_NOT_FOUND("ATTENDANCE_002", "Attendance record not found", 404),
    DUPLICATE_RECORD("ATTENDANCE_003", "Attendance record already exists for this student", 409),
    INVALID_CODE("ATTENDANCE_005", "Invalid attendance code", 400),
    CODE_EXPIRED("ATTENDANCE_006", "Attendance code has expired", 400),
    STUDENT_NOT_FOUND("ATTENDANCE_007", "Student not found", 404);

    private final String code;
    private final String message;
    private final int status;
}
