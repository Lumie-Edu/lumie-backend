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
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AttendanceStatisticsQueryService {

    private static final ZoneId REPORTING_ZONE = ZoneId.of("Asia/Seoul");

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;

    public AttendanceDashboardResponse getDashboardStatistics() {
        log.debug("Getting attendance dashboard statistics");

        LocalDate today = LocalDate.now(REPORTING_ZONE);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return getDashboardStatistics(weekStart, today);
    }

    public AttendanceDashboardResponse getDashboardStatistics(int months) {
        int window = Math.max(1, Math.min(months, 24));
        LocalDate today = LocalDate.now(REPORTING_ZONE);
        LocalDate firstDay = YearMonth.now(REPORTING_ZONE)
                .minusMonths(window - 1L)
                .atDay(1);
        return getDashboardStatistics(firstDay, today);
    }

    private AttendanceDashboardResponse getDashboardStatistics(LocalDate from, LocalDate to) {
        long sessions = sessionRepository.countBySessionDateBetween(from, to);
        long totalCount = recordRepository.countBySessionSessionDateBetweenAndStatusNot(
                from, to, AttendanceStatus.PENDING);
        long presentCount = recordRepository.countBySessionSessionDateBetweenAndStatus(
                from, to, AttendanceStatus.PRESENT);

        double attendanceRate = totalCount > 0
                ? Math.round((double) presentCount / totalCount * 1000.0) / 10.0
                : 0.0;

        return new AttendanceDashboardResponse(
                (int) sessions,
                attendanceRate,
                (int) presentCount,
                (int) totalCount
        );
    }
}
