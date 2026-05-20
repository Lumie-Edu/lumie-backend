package com.lumie.attendance.application.dto.request;

import com.lumie.attendance.domain.vo.AttendanceStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BulkUpdateStatusRequest(
    @NotEmpty(message = "Record IDs are required")
    List<Long> recordIds,

    @NotNull(message = "Status is required")
    AttendanceStatus status
) {
}
