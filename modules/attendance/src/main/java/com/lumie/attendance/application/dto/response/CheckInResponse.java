package com.lumie.attendance.application.dto.response;

import com.lumie.attendance.domain.vo.AttendanceStatus;

public record CheckInResponse(
    String message,
    AttendanceStatus status
) {
}
