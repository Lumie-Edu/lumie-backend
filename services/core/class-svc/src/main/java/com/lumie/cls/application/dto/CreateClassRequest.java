package com.lumie.cls.application.dto;

import com.lumie.cls.domain.vo.SchedulePattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateClassRequest(
    @NotBlank(message = "Class name is required")
    String name,

    @NotNull(message = "Academy ID is required")
    Long academyId,

    String subject,
    Long teacherId,
    Integer maxStudents,
    Long tuitionAmount,
    SchedulePattern schedulePattern,
    LocalDate startDate,
    LocalDate endDate,
    String description
) {
}
