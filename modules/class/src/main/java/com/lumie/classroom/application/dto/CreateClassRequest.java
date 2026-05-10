package com.lumie.classroom.application.dto;

import com.lumie.classroom.domain.vo.SchedulePattern;
import jakarta.validation.constraints.NotBlank;

public record CreateClassRequest(
    @NotBlank(message = "Class name is required")
    String name,

    Long teacherId,
    SchedulePattern schedulePattern,
    String description
) {
}
