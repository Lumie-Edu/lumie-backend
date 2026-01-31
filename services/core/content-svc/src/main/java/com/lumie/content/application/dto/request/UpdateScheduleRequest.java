package com.lumie.content.application.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateScheduleRequest(
        LocalDate scheduleDate,
        LocalTime startTime,
        LocalTime endTime,
        Integer slotDurationMinutes,
        Integer maxReservations,
        Boolean isAvailable
) {
}
