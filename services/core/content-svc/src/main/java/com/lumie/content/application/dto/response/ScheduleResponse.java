package com.lumie.content.application.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lumie.content.domain.entity.Schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ScheduleResponse(
        Long id,
        Long adminId,
        LocalDate date,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        Boolean isAvailable,
        Boolean hasReservation,
        Integer confirmedCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ScheduleResponse from(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getAdminId(),
                schedule.getDate(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getIsAvailable(),
                schedule.hasReservation(),
                schedule.getConfirmedCount(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }
}
