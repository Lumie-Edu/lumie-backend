package com.lumie.attendance.application.dto.response;

public record ClassStudentAttendanceResponse(
    Long studentId,
    String studentName,
    int totalSessions,
    int presentCount,
    int absentCount,
    int lateCount,
    int excusedCount,
    double attendanceRate
) {}
