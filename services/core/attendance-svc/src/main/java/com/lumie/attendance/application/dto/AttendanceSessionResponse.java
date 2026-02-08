package com.lumie.attendance.application.dto;

import com.lumie.attendance.domain.entity.AttendanceRecord;
import com.lumie.attendance.domain.entity.AttendanceSession;
import com.lumie.attendance.domain.vo.AttendanceStatus;
import com.lumie.attendance.domain.vo.SessionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AttendanceSessionResponse(
    Long id,
    Long academyId,
    String name,
    LocalDate sessionDate,
    String subject,
    String attendanceCode,
    LocalDateTime codeExpiresAt,
    Integer lateThresholdMinutes,
    SessionStatus status,
    Long createdBy,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    int presentCount,
    int absentCount,
    int lateCount,
    int excusedCount,
    int totalStudents
) {
    public static AttendanceSessionResponse from(AttendanceSession session) {
        List<AttendanceRecord> records = session.getRecords();
        int presentCount = 0;
        int absentCount = 0;
        int lateCount = 0;
        int excusedCount = 0;

        if (records != null) {
            for (AttendanceRecord record : records) {
                switch (record.getStatus()) {
                    case PRESENT -> presentCount++;
                    case ABSENT -> absentCount++;
                    case LATE -> lateCount++;
                    case EXCUSED -> excusedCount++;
                }
            }
        }

        int totalStudents = records != null ? records.size() : 0;

        return new AttendanceSessionResponse(
            session.getId(),
            session.getAcademyId(),
            session.getName(),
            session.getSessionDate(),
            session.getSubject(),
            session.getAttendanceCode(),
            session.getCodeExpiresAt(),
            session.getLateThresholdMinutes(),
            session.getStatus(),
            session.getCreatedBy(),
            session.getCreatedAt(),
            session.getUpdatedAt(),
            presentCount,
            absentCount,
            lateCount,
            excusedCount,
            totalStudents
        );
    }

    public static AttendanceSessionResponse fromWithoutRecords(AttendanceSession session) {
        return new AttendanceSessionResponse(
            session.getId(),
            session.getAcademyId(),
            session.getName(),
            session.getSessionDate(),
            session.getSubject(),
            session.getAttendanceCode(),
            session.getCodeExpiresAt(),
            session.getLateThresholdMinutes(),
            session.getStatus(),
            session.getCreatedBy(),
            session.getCreatedAt(),
            session.getUpdatedAt(),
            0, 0, 0, 0, 0
        );
    }
}
