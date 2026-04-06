package com.lumie.cls.application.dto;

import com.lumie.cls.domain.vo.SchedulePattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateClassRequest(
    @NotBlank(message = "Class name is required")
    String name,

    @NotNull(message = "Academy ID is required")
    Long academyId,

    String subject,
    Long teacherId,
    SchedulePattern schedulePattern,
    String description
) {
}
