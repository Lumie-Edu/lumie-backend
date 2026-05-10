package com.lumie.content.application.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lumie.content.domain.entity.Schedule;

import java.time.LocalDate;
import java.time.Instant;
import com.lumie.common.util.Timestamps;
import java.time.LocalTime;

public record ScheduleResponse(
        Long id,
        Long userId,
        LocalDate date,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        String description,
        Boolean isAvailable,
        Boolean hasReservation,
        Integer confirmedCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static ScheduleResponse from(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getUserId(),
                schedule.getDate(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getDescription(),
                schedule.getIsAvailable(),
                schedule.hasReservation(),
                schedule.getConfirmedCount(),
                Timestamps.utcInstant(schedule.getCreatedAt()),
                Timestamps.utcInstant(schedule.getUpdatedAt())
        );
    }
}
