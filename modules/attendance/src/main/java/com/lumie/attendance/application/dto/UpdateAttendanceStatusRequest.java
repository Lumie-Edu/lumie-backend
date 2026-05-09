package com.lumie.attendance.application.dto;

import com.lumie.attendance.domain.vo.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAttendanceStatusRequest(
    @NotNull(message = "Status is required")
    AttendanceStatus status,

    String memo
) {
}
