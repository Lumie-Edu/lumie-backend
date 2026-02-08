package com.lumie.admin.application.dto;

import com.lumie.admin.domain.vo.AttendanceStatus;

public record AttendanceRequest(
    Long adminId,
    AttendanceStatus status,
    Integer lateMinutes,
    Integer earlyLeaveMinutes,
    Integer overtimeMinutes,
    String note
) {
}
