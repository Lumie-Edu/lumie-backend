package com.lumie.attendance.application.dto.response;

import com.lumie.attendance.domain.entity.AttendanceRecord;
import com.lumie.attendance.domain.vo.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record AttendanceRecordResponse(
    Long id,
    Long sessionId,
    @Schema(nullable = true)
    Long studentId,
    String studentName,
    AttendanceStatus status,
    @Schema(nullable = true) Instant checkedAt,
    @Schema(nullable = true) String memo
) {
    public static AttendanceRecordResponse from(AttendanceRecord record, String studentName) {
        return new AttendanceRecordResponse(
            record.getId(),
            record.getSession().getId(),
            record.getStudentId(),
            studentName,
            record.getStatus(),
            record.getCheckedAt(),
            record.getMemo()
        );
    }
}
