package com.lumie.content.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;

public record CreateScheduleRequest(
        @NotNull(message = "Academy ID is required")
        Long academyId,

        @NotNull(message = "Admin ID is required")
        Long adminId,

        @NotNull(message = "Schedule date is required")
        LocalDate scheduleDate,

        @NotNull(message = "Start time is required")
        LocalTime startTime,

        @NotNull(message = "End time is required")
        LocalTime endTime,

        @Positive(message = "Slot duration must be positive")
        Integer slotDurationMinutes,

        @Positive(message = "Max reservations must be positive")
        Integer maxReservations
) {
}
