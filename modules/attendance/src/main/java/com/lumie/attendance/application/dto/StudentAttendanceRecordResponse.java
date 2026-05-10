package com.lumie.attendance.application.dto;

import com.lumie.attendance.domain.entity.AttendanceRecord;
import com.lumie.attendance.domain.vo.AttendanceStatus;

import java.time.LocalDate;
import java.time.Instant;

public record StudentAttendanceRecordResponse(
    Long id,
    Long sessionId,
    String sessionName,
    LocalDate sessionDate,
    Long studentId,
    String studentName,
    AttendanceStatus status,
    Instant checkedAt,
    String memo
) {
    public static StudentAttendanceRecordResponse from(AttendanceRecord record, String studentName) {
        return new StudentAttendanceRecordResponse(
            record.getId(),
            record.getSession().getId(),
            record.getSession().getName(),
            record.getSession().getSessionDate(),
            record.getStudentId(),
            studentName,
            record.getStatus(),
            record.getCheckedAt(),
            record.getMemo()
        );
    }
}
