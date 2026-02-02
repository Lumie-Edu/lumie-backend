package com.lumie.content.application.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateReservationRequest(
        @NotNull(message = "Schedule ID is required")
        Long scheduleId,

        @NotNull(message = "Student ID is required")
        Long studentId,

        @NotNull(message = "Admin ID is required")
        Long adminId,

        String consultationContent
) {
}
