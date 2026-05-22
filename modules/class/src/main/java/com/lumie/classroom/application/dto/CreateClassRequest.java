package com.lumie.classroom.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record CreateClassRequest(
    @NotBlank(message = "Class name is required")
    String name,

    Long teacherId,

    @Valid
    SchedulePatternRequest schedulePattern,

    String description
) {
}
