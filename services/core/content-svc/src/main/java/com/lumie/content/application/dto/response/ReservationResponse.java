package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.Reservation;
import com.lumie.content.domain.vo.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long scheduleId,
        LocalDate date,
        Integer timeSlotId,
        Long studentId,
        Long adminId,
        String consultationContent,
        ReservationStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getSchedule().getId(),
                reservation.getSchedule().getDate(),
                reservation.getSchedule().getTimeSlotId(),
                reservation.getStudentId(),
                reservation.getAdminId(),
                reservation.getConsultationContent(),
                reservation.getStatus(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt()
        );
    }
}
