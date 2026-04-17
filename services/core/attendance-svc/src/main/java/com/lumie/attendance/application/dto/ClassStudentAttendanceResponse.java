package com.lumie.attendance.application.dto;

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
