package com.lumie.admin.application.dto;

import com.lumie.admin.domain.entity.Attendance;
import com.lumie.admin.domain.vo.AttendanceStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AttendanceResponse(
    Long id,
    Long adminId,
    String adminName,
    LocalDate workDate,
    LocalDateTime clockInTime,
    LocalDateTime clockOutTime,
    AttendanceStatus status,
    Integer lateMinutes,
    Integer earlyLeaveMinutes,
    Integer overtimeMinutes,
    String note
) {
    public static AttendanceResponse from(Attendance attendance) {
        return new AttendanceResponse(
            attendance.getId(),
            attendance.getAdmin().getId(),
            attendance.getAdmin().getAdminName(),
            attendance.getWorkDate(),
            attendance.getClockInTime(),
            attendance.getClockOutTime(),
            attendance.getStatus(),
            attendance.getLateMinutes(),
            attendance.getEarlyLeaveMinutes(),
            attendance.getOvertimeMinutes(),
            attendance.getNote()
        );
    }
}
