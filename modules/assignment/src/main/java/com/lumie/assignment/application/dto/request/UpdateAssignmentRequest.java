package com.lumie.assignment.application.dto.request;

import java.time.Instant;
import java.util.List;

public record UpdateAssignmentRequest(
    String title,
    String description,
    String type,
    Instant dueDate,
    Integer maxScore,
    List<Long> attachments,
    String status
) {
}
