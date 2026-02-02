package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.Schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ScheduleResponse(
        Long id,
        Long adminId,
        LocalDate date,
        Integer timeSlotId,
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
                schedule.getTimeSlotId(),
                schedule.getIsAvailable(),
                schedule.hasReservation(),
                schedule.getConfirmedCount(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }
}
