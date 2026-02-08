package com.lumie.admin.application.dto;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

public record WorkScheduleRequest(
    @NotNull(message = "Day of week is required")
    DayOfWeek dayOfWeek,

    @NotNull(message = "Start time is required")
    LocalTime startTime,

    @NotNull(message = "End time is required")
    LocalTime endTime,

    LocalTime breakStartTime,
    LocalTime breakEndTime,

    @NotNull(message = "Effective from date is required")
    LocalDate effectiveFrom,

    LocalDate effectiveTo,
    Boolean isDayOff
) {
}
