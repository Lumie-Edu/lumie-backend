package com.lumie.content.application.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lumie.content.domain.entity.Schedule;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.Instant;
import java.time.LocalTime;

public record ScheduleResponse(
        Long id,
        Long userId,
        LocalDate date,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        @Schema(nullable = true) String description,
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
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }
}
