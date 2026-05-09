package com.lumie.attendance.application.service;

import com.lumie.attendance.application.dto.response.statistics.AttendanceDashboardResponse;
import com.lumie.attendance.domain.repository.AttendanceRecordRepository;
import com.lumie.attendance.domain.repository.AttendanceSessionRepository;
import com.lumie.attendance.domain.vo.AttendanceStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AttendanceStatisticsQueryService {

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;

    public AttendanceDashboardResponse getDashboardStatistics() {
        log.debug("Getting attendance dashboard statistics");

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = today;

        long sessionsThisWeek = sessionRepository.countBySessionDateBetween(weekStart, weekEnd);
        long totalCount = recordRepository.countBySessionSessionDateBetween(weekStart, weekEnd);
        long presentCount = recordRepository.countBySessionSessionDateBetweenAndStatus(
                weekStart, weekEnd, AttendanceStatus.PRESENT);

        double attendanceRate = totalCount > 0
                ? Math.round((double) presentCount / totalCount * 1000.0) / 10.0
                : 0.0;

        return new AttendanceDashboardResponse(
                (int) sessionsThisWeek,
                attendanceRate,
                (int) presentCount,
                (int) totalCount
        );
    }
}
