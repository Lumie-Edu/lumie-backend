package com.lumie.content.application.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateScheduleRequest(
        @NotNull(message = "Date is required")
        LocalDate date,

        @NotNull(message = "Time slot ID is required")
        Integer timeSlotId
) {
}
