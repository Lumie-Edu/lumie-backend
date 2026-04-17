package com.lumie.attendance.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AttendanceSessionRequest(
    @NotBlank(message = "Session name is required")
    String name,

    @NotNull(message = "Session date is required")
    LocalDate sessionDate,

    @NotNull(message = "Academy ID is required")
    Long academyId,

    Long classId,

    Integer lateThresholdMinutes
) {
}
