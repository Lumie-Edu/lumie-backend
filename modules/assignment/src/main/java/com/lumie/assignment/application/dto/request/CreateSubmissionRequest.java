package com.lumie.assignment.application.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record CreateSubmissionRequest(
    @NotNull(message = "Student ID is required")
    Long studentId,

    String content,
    List<Long> attachments,
    Map<String, String> answers
) {
    public CreateSubmissionRequest(Long studentId, String content, List<Long> attachments) {
        this(studentId, content, attachments, null);
    }
}
