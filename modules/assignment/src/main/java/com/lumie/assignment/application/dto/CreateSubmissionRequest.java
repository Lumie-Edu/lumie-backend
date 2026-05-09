package com.lumie.assignment.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateSubmissionRequest(
    @NotNull(message = "Student ID is required")
    Long studentId,

    String content,
    List<Long> attachments
) {
}
