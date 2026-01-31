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
        Long studentId,
        LocalTime reservationTime,
        String topic,
        String notes,
        ReservationStatus status,
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
                reservation.getStudentId(),
                reservation.getReservationTime(),
                reservation.getTopic(),
                reservation.getNotes(),
                reservation.getStatus(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt()
        );
    }
}
