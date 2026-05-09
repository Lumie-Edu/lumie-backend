package com.lumie.cls.application.dto;

import com.lumie.cls.domain.vo.SchedulePattern;
import jakarta.validation.constraints.NotBlank;

public record CreateClassRequest(
    @NotBlank(message = "Class name is required")
    String name,

    Long teacherId,
    SchedulePattern schedulePattern,
    String description
) {
}
