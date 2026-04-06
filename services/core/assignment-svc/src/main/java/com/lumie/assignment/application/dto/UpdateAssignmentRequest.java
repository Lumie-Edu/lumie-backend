package com.lumie.assignment.application.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UpdateAssignmentRequest(
    String title,
    String description,
    String type,
    LocalDateTime dueDate,
    Integer maxScore,
    List<Long> attachments,
    String status
) {
}
