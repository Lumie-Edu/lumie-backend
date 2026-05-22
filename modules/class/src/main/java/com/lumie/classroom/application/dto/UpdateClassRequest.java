package com.lumie.classroom.application.dto;

import jakarta.validation.Valid;

public record UpdateClassRequest(
    String name,
    Long teacherId,

    @Valid
    SchedulePatternRequest schedulePattern,

    String description
) {
}
