package com.lumie.content.application.service;

import com.lumie.content.domain.repository.ReservationRepository;
import com.lumie.content.domain.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeletionReadinessService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ReservationRepository reservationRepository;
    private final ScheduleRepository scheduleRepository;

    public boolean hasActiveOrUpcomingReservationsForStudent(Long studentId) {
        return reservationRepository.existsActiveOrUpcomingByStudentId(studentId);
    }

    public boolean hasUpcomingSchedulesForUser(Long userId) {
        return scheduleRepository.existsByUserIdAndDateGreaterThanEqual(userId, LocalDate.now(KST));
    }
}
