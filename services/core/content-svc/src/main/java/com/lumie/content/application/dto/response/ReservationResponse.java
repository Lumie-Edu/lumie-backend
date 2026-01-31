package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.Reservation;
import com.lumie.content.domain.vo.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ReservationResponse(
        Long id,
        Long scheduleId,
        LocalDate scheduleDate,
        LocalTime startTime,
        LocalTime endTime,
        String adminName,
        Long studentId,
        String studentName,
        String studentPhone,
        ReservationStatus status,
        String memo,
        String cancelReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getSchedule().getId(),
                reservation.getSchedule().getScheduleDate(),
                reservation.getSchedule().getStartTime(),
                reservation.getSchedule().getEndTime(),
                reservation.getSchedule().getAdminName(),
                reservation.getStudentId(),
                reservation.getStudentName(),
                reservation.getStudentPhone(),
                reservation.getStatus(),
                reservation.getMemo(),
                reservation.getCancelReason(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt()
        );
    }
}
