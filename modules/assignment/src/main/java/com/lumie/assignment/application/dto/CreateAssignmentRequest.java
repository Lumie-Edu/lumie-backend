package com.lumie.assignment.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record CreateAssignmentRequest(
    @NotNull(message = "Class ID is required")
    Long classId,

    @NotBlank(message = "Title is required")
    String title,

    String description,
    String type,
    Instant dueDate,
    Integer maxScore,
    List<Long> attachments
) {
}
