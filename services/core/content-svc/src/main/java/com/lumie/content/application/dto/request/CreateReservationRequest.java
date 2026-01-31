package com.lumie.content.application.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record CreateReservationRequest(
        @NotNull(message = "Schedule ID is required")
        Long scheduleId,

        @NotNull(message = "Student ID is required")
        Long studentId,

        @NotNull(message = "Reservation time is required")
        LocalTime reservationTime,

        String topic,

        String notes
) {
}
