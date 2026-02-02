package com.lumie.content.application.service;

import com.lumie.content.application.dto.request.CreateReservationRequest;
import com.lumie.content.application.dto.request.UpdateReservationStatusRequest;
import com.lumie.content.application.dto.response.ReservationResponse;
import com.lumie.content.application.port.out.ContentEventPublisherPort;
import com.lumie.content.domain.entity.Reservation;
import com.lumie.content.domain.entity.Schedule;
import com.lumie.content.domain.exception.ContentErrorCode;
import com.lumie.content.domain.exception.ContentException;
import com.lumie.content.domain.repository.ReservationRepository;
import com.lumie.content.domain.repository.ScheduleRepository;
import com.lumie.content.domain.vo.ReservationStatus;
import com.lumie.content.infrastructure.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationCommandService {

    private final ReservationRepository reservationRepository;
    private final ScheduleRepository scheduleRepository;
    private final ContentEventPublisherPort eventPublisher;

    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest request) {
        log.info("Creating reservation for schedule: {} by student: {}",
                request.scheduleId(), request.studentId());

        Schedule schedule = scheduleRepository.findByIdWithReservations(request.scheduleId())
                .orElseThrow(() -> new ContentException(ContentErrorCode.SCHEDULE_NOT_FOUND));

        // Check if schedule is available
        if (!schedule.getIsAvailable()) {
            throw new ContentException(ContentErrorCode.SCHEDULE_NOT_AVAILABLE);
        }

        // Check if schedule can accept more reservations
        if (!schedule.canAcceptReservation()) {
            throw new ContentException(ContentErrorCode.SCHEDULE_FULL);
        }

        // Check for duplicate reservation
        boolean hasPendingOrConfirmed = reservationRepository.existsByScheduleIdAndStudentIdAndStatusNot(
                request.scheduleId(), request.studentId(), ReservationStatus.CANCELLED);
        if (hasPendingOrConfirmed) {
            throw new ContentException(ContentErrorCode.DUPLICATE_RESERVATION);
        }

        Reservation reservation = Reservation.create(
                schedule,
                request.studentId(),
                request.adminId(),
                request.consultationContent()
        );

        Reservation saved = reservationRepository.save(reservation);
        log.info("Reservation created with id: {}", saved.getId());

        return ReservationResponse.from(saved);
    }

    @Transactional
    public ReservationResponse updateReservationStatus(Long id, UpdateReservationStatusRequest request) {
        log.info("Updating reservation {} status to: {}", id, request.status());

        Reservation reservation = reservationRepository.findByIdWithSchedule(id)
                .orElseThrow(() -> new ContentException(ContentErrorCode.RESERVATION_NOT_FOUND));

        ReservationStatus currentStatus = reservation.getStatus();
        ReservationStatus newStatus = request.status();

        // Validate status transition
        validateStatusTransition(currentStatus, newStatus);

        switch (newStatus) {
            case CONFIRMED -> {
                reservation.confirm();
                String tenantSlug = TenantContextHolder.getTenantSlug();
                eventPublisher.publishReservationConfirmed(reservation, tenantSlug);
            }
            case CANCELLED -> reservation.cancel();
            case COMPLETED -> reservation.complete();
            default -> throw new ContentException(ContentErrorCode.INVALID_RESERVATION_STATUS);
        }

        Reservation updated = reservationRepository.save(reservation);
        log.info("Reservation status updated to: {}", updated.getStatus());

        return ReservationResponse.from(updated);
    }

    @Transactional
    public void cancelReservation(Long id) {
        log.info("Cancelling reservation: {}", id);

        Reservation reservation = reservationRepository.findByIdWithSchedule(id)
                .orElseThrow(() -> new ContentException(ContentErrorCode.RESERVATION_NOT_FOUND));

        if (reservation.isCancelled()) {
            throw new ContentException(ContentErrorCode.INVALID_RESERVATION_STATUS,
                    "Reservation is already cancelled");
        }

        if (reservation.isCompleted()) {
            throw new ContentException(ContentErrorCode.INVALID_RESERVATION_STATUS,
                    "Cannot cancel completed reservation");
        }

        reservation.cancel();
        reservationRepository.save(reservation);
        log.info("Reservation cancelled: {}", id);
    }

    private void validateStatusTransition(ReservationStatus current, ReservationStatus target) {
        boolean valid = switch (current) {
            case PENDING -> target == ReservationStatus.CONFIRMED || target == ReservationStatus.CANCELLED;
            case CONFIRMED -> target == ReservationStatus.COMPLETED || target == ReservationStatus.CANCELLED;
            case CANCELLED, COMPLETED -> false;
        };

        if (!valid) {
            throw new ContentException(ContentErrorCode.INVALID_RESERVATION_STATUS,
                    String.format("Cannot transition from %s to %s", current, target));
        }
    }
}
