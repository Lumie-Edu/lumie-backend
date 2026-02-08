package com.lumie.admin.application.service;

import com.lumie.admin.application.dto.AttendanceRequest;
import com.lumie.admin.application.dto.AttendanceResponse;
import com.lumie.admin.domain.entity.Admin;
import com.lumie.admin.domain.entity.Attendance;
import com.lumie.admin.domain.entity.WorkSchedule;
import com.lumie.admin.domain.exception.AdminErrorCode;
import com.lumie.admin.domain.exception.AdminNotFoundException;
import com.lumie.admin.domain.exception.AttendanceNotFoundException;
import com.lumie.admin.domain.repository.AdminRepository;
import com.lumie.admin.domain.repository.AttendanceRepository;
import com.lumie.admin.domain.repository.WorkScheduleRepository;
import com.lumie.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceCommandService {

    private final AttendanceRepository attendanceRepository;
    private final AdminRepository adminRepository;
    private final WorkScheduleRepository workScheduleRepository;

    public AttendanceResponse clockIn(Long adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new AdminNotFoundException(adminId));

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        if (attendanceRepository.findByAdminIdAndWorkDate(adminId, today).isPresent()) {
            throw new BusinessException(AdminErrorCode.ALREADY_CLOCKED_IN, "Already clocked in today");
        }

        LocalTime scheduledStart = getScheduledStartTime(adminId, today);

        Attendance attendance = Attendance.builder()
                .admin(admin)
                .workDate(today)
                .build();
        attendance.clockIn(now, scheduledStart);

        Attendance saved = attendanceRepository.save(attendance);
        log.info("Admin {} clocked in at {}", adminId, now);
        return AttendanceResponse.from(saved);
    }

    public AttendanceResponse clockOut(Long adminId) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        Attendance attendance = attendanceRepository.findByAdminIdAndWorkDate(adminId, today)
                .orElseThrow(() -> new BusinessException(AdminErrorCode.NOT_CLOCKED_IN, "Not clocked in yet"));

        LocalTime scheduledEnd = getScheduledEndTime(adminId, today);
        attendance.clockOut(now, scheduledEnd);

        Attendance saved = attendanceRepository.save(attendance);
        log.info("Admin {} clocked out at {}", adminId, now);
        return AttendanceResponse.from(saved);
    }

    public AttendanceResponse updateAttendance(Long id, AttendanceRequest request) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new AttendanceNotFoundException(id));
        attendance.update(request.status(), request.lateMinutes(), request.earlyLeaveMinutes(),
                request.overtimeMinutes(), request.note());
        Attendance updated = attendanceRepository.save(attendance);
        log.info("Attendance {} updated", id);
        return AttendanceResponse.from(updated);
    }

    private LocalTime getScheduledStartTime(Long adminId, LocalDate date) {
        List<WorkSchedule> schedules = workScheduleRepository.findByAdminIdAndEffectiveDate(adminId, date);
        return schedules.stream()
                .filter(s -> s.getDayOfWeek() == date.getDayOfWeek())
                .filter(s -> !Boolean.TRUE.equals(s.getIsDayOff()))
                .findFirst()
                .map(WorkSchedule::getStartTime)
                .orElse(null);
    }

    private LocalTime getScheduledEndTime(Long adminId, LocalDate date) {
        List<WorkSchedule> schedules = workScheduleRepository.findByAdminIdAndEffectiveDate(adminId, date);
        return schedules.stream()
                .filter(s -> s.getDayOfWeek() == date.getDayOfWeek())
                .filter(s -> !Boolean.TRUE.equals(s.getIsDayOff()))
                .findFirst()
                .map(WorkSchedule::getEndTime)
                .orElse(null);
    }
}
