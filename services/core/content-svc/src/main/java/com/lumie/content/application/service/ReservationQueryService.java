package com.lumie.content.application.service;

import com.lumie.content.application.dto.response.ReservationResponse;
import com.lumie.content.domain.entity.Reservation;
import com.lumie.content.domain.exception.ContentErrorCode;
import com.lumie.content.domain.exception.ContentException;
import com.lumie.content.domain.repository.ReservationRepository;
import com.lumie.content.domain.vo.ReservationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReservationQueryService {

    private final ReservationRepository reservationRepository;

    public Page<ReservationResponse> listReservations(Pageable pageable) {
        log.debug("Listing reservations with pagination");

        return reservationRepository.findAll(pageable)
                .map(ReservationResponse::from);
    }

    public List<ReservationResponse> listReservationsByStudent(Long studentId) {
        log.debug("Listing reservations for student: {}", studentId);

        return reservationRepository.findByStudentId(studentId).stream()
                .map(ReservationResponse::from)
                .toList();
    }

    public List<ReservationResponse> listReservationsByStudentAndStatus(Long studentId, ReservationStatus status) {
        log.debug("Listing reservations for student: {} with status: {}", studentId, status);

        return reservationRepository.findByStudentIdAndStatus(studentId, status).stream()
                .map(ReservationResponse::from)
                .toList();
    }

    public List<ReservationResponse> listReservationsBySchedule(Long scheduleId) {
        log.debug("Listing reservations for schedule: {}", scheduleId);

        return reservationRepository.findByScheduleId(scheduleId).stream()
                .map(ReservationResponse::from)
                .toList();
    }

    public ReservationResponse getReservation(Long id) {
        log.debug("Getting reservation: {}", id);

        Reservation reservation = reservationRepository.findByIdWithSchedule(id)
                .orElseThrow(() -> new ContentException(ContentErrorCode.RESERVATION_NOT_FOUND));

        return ReservationResponse.from(reservation);
    }
}
