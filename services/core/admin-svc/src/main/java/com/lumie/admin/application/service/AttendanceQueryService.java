package com.lumie.admin.application.service;

import com.lumie.admin.application.dto.AttendanceResponse;
import com.lumie.admin.application.dto.AttendanceSummaryResponse;
import com.lumie.admin.domain.entity.Attendance;
import com.lumie.admin.domain.exception.AttendanceNotFoundException;
import com.lumie.admin.domain.repository.AttendanceRepository;
import com.lumie.admin.domain.vo.AttendanceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceQueryService {

    private final AttendanceRepository attendanceRepository;

    public AttendanceResponse getAttendance(Long id) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new AttendanceNotFoundException(id));
        return AttendanceResponse.from(attendance);
    }

    public Page<AttendanceResponse> getAttendances(Long adminId, LocalDate dateFrom,
                                                    LocalDate dateTo, AttendanceStatus status,
                                                    Pageable pageable) {
        return attendanceRepository.findByFilters(adminId, dateFrom, dateTo, status, pageable)
                .map(AttendanceResponse::from);
    }

    public AttendanceSummaryResponse getMonthlySummary(Long adminId, YearMonth yearMonth) {
        LocalDate from = yearMonth.atDay(1);
        LocalDate to = yearMonth.atEndOfMonth();
        List<Attendance> attendances = attendanceRepository.findByAdminIdAndWorkDateBetween(adminId, from, to);

        int presentDays = 0, lateDays = 0, absentDays = 0, dayOffDays = 0, halfDayDays = 0;
        int totalLateMinutes = 0, totalEarlyLeaveMinutes = 0, totalOvertimeMinutes = 0;

        for (Attendance a : attendances) {
            switch (a.getStatus()) {
                case PRESENT -> presentDays++;
                case LATE -> { lateDays++; presentDays++; }
                case ABSENT -> absentDays++;
                case DAY_OFF -> dayOffDays++;
                case HALF_DAY -> halfDayDays++;
                default -> {}
            }
            totalLateMinutes += a.getLateMinutes() != null ? a.getLateMinutes() : 0;
            totalEarlyLeaveMinutes += a.getEarlyLeaveMinutes() != null ? a.getEarlyLeaveMinutes() : 0;
            totalOvertimeMinutes += a.getOvertimeMinutes() != null ? a.getOvertimeMinutes() : 0;
        }

        return new AttendanceSummaryResponse(
            adminId,
            yearMonth.getYear() * 100 + yearMonth.getMonthValue(),
            attendances.size(),
            presentDays, lateDays, absentDays, dayOffDays, halfDayDays,
            totalLateMinutes, totalEarlyLeaveMinutes, totalOvertimeMinutes
        );
    }
}
