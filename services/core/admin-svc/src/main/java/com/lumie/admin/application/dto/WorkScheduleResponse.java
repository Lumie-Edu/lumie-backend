package com.lumie.admin.application.dto;

import com.lumie.admin.domain.entity.WorkSchedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

public record WorkScheduleResponse(
    Long id,
    Long adminId,
    DayOfWeek dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    LocalTime breakStartTime,
    LocalTime breakEndTime,
    LocalDate effectiveFrom,
    LocalDate effectiveTo,
    Boolean isDayOff,
    Long workMinutes
) {
    public static WorkScheduleResponse from(WorkSchedule schedule) {
        return new WorkScheduleResponse(
            schedule.getId(),
            schedule.getAdmin().getId(),
            schedule.getDayOfWeek(),
            schedule.getStartTime(),
            schedule.getEndTime(),
            schedule.getBreakStartTime(),
            schedule.getBreakEndTime(),
            schedule.getEffectiveFrom(),
            schedule.getEffectiveTo(),
            schedule.getIsDayOff(),
            schedule.calculateWorkMinutes()
        );
    }
}
