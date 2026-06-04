package com.lumie.attendance.domain.vo;

public enum AttendanceStatus {
    PRESENT,
    ABSENT,
    LATE,
    EXCUSED,
    // Default state of a freshly-created record: attendance not yet decided ("미인증").
    // Excluded from attendance-rate denominators until a teacher or check-in resolves it.
    PENDING
}
