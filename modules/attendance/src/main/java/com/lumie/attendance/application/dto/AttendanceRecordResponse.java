package com.lumie.attendance.application.dto;

import com.lumie.attendance.domain.entity.AttendanceRecord;
import com.lumie.attendance.domain.vo.AttendanceStatus;

import java.time.Instant;
import com.lumie.common.util.Timestamps;

public record AttendanceRecordResponse(
    Long id,
    Long sessionId,
    Long studentId,
    String studentName,
    AttendanceStatus status,
    Instant checkedAt,
    String memo
) {
    public static AttendanceRecordResponse from(AttendanceRecord record, String studentName) {
        return new AttendanceRecordResponse(
            record.getId(),
            record.getSession().getId(),
            record.getStudentId(),
            studentName,
            record.getStatus(),
            Timestamps.utcInstant(record.getCheckedAt()),
            record.getMemo()
        );
    }
}
