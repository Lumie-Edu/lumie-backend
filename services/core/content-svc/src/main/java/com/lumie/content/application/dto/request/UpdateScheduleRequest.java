package com.lumie.content.application.dto.request;

import java.time.LocalDate;

public record UpdateScheduleRequest(
        LocalDate date,
        Integer timeSlotId,
        Boolean isAvailable
) {
}
