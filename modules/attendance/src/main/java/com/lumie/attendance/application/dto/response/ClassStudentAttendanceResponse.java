package com.lumie.attendance.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ClassStudentAttendanceResponse(
    @Schema(nullable = true)
    Long studentId,
    String studentName,
    int totalSessions,
    int presentCount,
    int absentCount,
    int lateCount,
    int excusedCount,
    double attendanceRate
) {}
