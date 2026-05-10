package com.lumie.assignment.application.dto;

import java.time.Instant;
import com.lumie.common.util.Timestamps;
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
