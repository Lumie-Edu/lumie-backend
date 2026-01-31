package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.Schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ScheduleResponse(
        Long id,
        Long adminId,
        String adminName,
        LocalDate scheduleDate,
        LocalTime startTime,
        LocalTime endTime,
        Integer maxReservations,
        Integer availableSlots,
        Integer confirmedCount,
        String description,
        Boolean isAvailable,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ScheduleResponse from(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getAdminId(),
                schedule.getAdminName(),
                schedule.getScheduleDate(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getMaxReservations(),
                schedule.getAvailableSlots(),
                schedule.getConfirmedCount(),
                schedule.getDescription(),
                schedule.getIsAvailable(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }
}
