package com.lumie.attendance.application.dto;

import com.lumie.attendance.domain.vo.AttendanceStatus;

public record CheckInResponse(
    String message,
    AttendanceStatus status
) {
}
