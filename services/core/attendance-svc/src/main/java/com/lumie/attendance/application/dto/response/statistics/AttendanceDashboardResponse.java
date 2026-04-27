package com.lumie.attendance.application.dto.response.statistics;

public record AttendanceDashboardResponse(
        int sessionsThisWeek,
        double attendanceRate,
        int presentCount,
        int totalCount
) {
}
