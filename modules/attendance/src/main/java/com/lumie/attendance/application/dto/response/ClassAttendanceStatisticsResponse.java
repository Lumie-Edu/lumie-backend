package com.lumie.attendance.application.dto.response;

import java.util.List;

public record ClassAttendanceStatisticsResponse(
    Long classId,
    String className,
    int totalSessions,
    int totalStudents,
    int presentCount,
    int absentCount,
    int lateCount,
    int excusedCount,
    double attendanceRate,
    List<WeeklyRate> weeklyTrend
) {
    public record WeeklyRate(
        String weekLabel,
        double rate
    ) {}
}
