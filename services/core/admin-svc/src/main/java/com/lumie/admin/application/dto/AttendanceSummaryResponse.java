package com.lumie.admin.application.dto;

public record AttendanceSummaryResponse(
    Long adminId,
    int yearMonth,
    int totalWorkDays,
    int presentDays,
    int lateDays,
    int absentDays,
    int dayOffDays,
    int halfDayDays,
    int totalLateMinutes,
    int totalEarlyLeaveMinutes,
    int totalOvertimeMinutes
) {
}
