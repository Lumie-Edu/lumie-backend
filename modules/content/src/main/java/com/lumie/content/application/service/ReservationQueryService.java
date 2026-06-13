package com.lumie.content.application.service;

import com.lumie.content.application.dto.response.ReservationResponse;
import com.lumie.content.application.dto.response.TodayReservationResponse;
import com.lumie.content.domain.entity.Reservation;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.content.domain.exception.ContentErrorCode;
import com.lumie.content.domain.repository.ReservationRepository;
import com.lumie.content.domain.vo.ReservationStatus;
import com.lumie.student.api.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReservationQueryService {

    private static final String DELETED_STUDENT_NAME = "삭제된 학생";

    private final ReservationRepository reservationRepository;
    private final StudentService studentService;

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

    public List<TodayReservationResponse> listTodayReservations() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        log.debug("Listing reservations for date: {}", today);

        List<Reservation> reservations = reservationRepository.findByScheduleDateExcludingCancelled(today);
        if (reservations.isEmpty()) {
            return List.of();
        }
        List<Long> studentIds = reservations.stream()
                .map(Reservation::getStudentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> nameById = studentIds.isEmpty()
                ? Map.of()
                : studentService.getStudentsByIds(TenantContextHolder.getRequiredTenant(), studentIds)
                        .students().stream()
                        .collect(Collectors.toMap(
                                StudentService.StudentData::id,
                                StudentService.StudentData::studentName
                        ));
        return reservations.stream()
                .map(r -> TodayReservationResponse.from(
                        r,
                        nameById.getOrDefault(r.getStudentId(), DELETED_STUDENT_NAME)))
                .toList();
    }

    public ReservationResponse getReservation(Long id) {
        log.debug("Getting reservation: {}", id);

        Reservation reservation = reservationRepository.findByIdWithSchedule(id)
                .orElseThrow(() -> new BusinessException(ContentErrorCode.RESERVATION_NOT_FOUND));

        return ReservationResponse.from(reservation);
    }
}
