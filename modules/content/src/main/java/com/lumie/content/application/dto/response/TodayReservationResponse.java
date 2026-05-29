package com.lumie.content.application.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lumie.content.domain.entity.Reservation;
import com.lumie.content.domain.vo.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record TodayReservationResponse(
        Long id,
        Long scheduleId,
        LocalDate date,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        Long studentId,
        @Schema(nullable = true) String studentName,
        @Schema(nullable = true) String consultationContent,
        ReservationStatus status,
        Instant createdAt
) {
    public static TodayReservationResponse from(Reservation reservation, String studentName) {
        return new TodayReservationResponse(
                reservation.getId(),
                reservation.getSchedule().getId(),
                reservation.getSchedule().getDate(),
                reservation.getSchedule().getStartTime(),
                reservation.getSchedule().getEndTime(),
                reservation.getStudentId(),
                studentName,
                reservation.getConsultationContent(),
                reservation.getStatus(),
                reservation.getCreatedAt()
        );
    }
}
