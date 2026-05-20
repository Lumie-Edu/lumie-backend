package com.lumie.attendance.application.dto.response;

public record StudentAttendanceStatisticsResponse(
    Long studentId,
    int totalSessions,
    int presentCount,
    int absentCount,
    int lateCount,
    int excusedCount,
    double attendanceRate
) {}
