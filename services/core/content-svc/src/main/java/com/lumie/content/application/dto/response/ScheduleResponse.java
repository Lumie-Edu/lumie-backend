package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.Schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ScheduleResponse(
        Long id,
        Long academyId,
        Long adminId,
        LocalDate scheduleDate,
        LocalTime startTime,
        LocalTime endTime,
        Integer slotDurationMinutes,
        Integer maxReservations,
        Integer availableSlots,
        Integer confirmedCount,
        Boolean isAvailable,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ScheduleResponse from(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getAcademyId(),
                schedule.getAdminId(),
                schedule.getScheduleDate(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getSlotDurationMinutes(),
                schedule.getMaxReservations(),
                schedule.getAvailableSlots(),
                schedule.getConfirmedCount(),
                schedule.getIsAvailable(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }
}
