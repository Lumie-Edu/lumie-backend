package com.lumie.assignment.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record CreateAssignmentRequest(
    @NotNull(message = "Class ID is required")
    Long classId,

    @NotNull(message = "Academy ID is required")
    Long academyId,

    @NotBlank(message = "Title is required")
    String title,

    String description,
    String type,
    LocalDateTime dueDate,
    Integer maxScore,
    List<Long> attachments,

    @NotNull(message = "Created by is required")
    Long createdBy
) {
}
