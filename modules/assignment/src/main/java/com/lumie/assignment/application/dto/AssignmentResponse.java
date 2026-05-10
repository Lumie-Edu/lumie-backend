package com.lumie.assignment.application.dto;

import com.lumie.assignment.domain.entity.Assignment;
import com.lumie.assignment.domain.vo.AssignmentStatus;
import com.lumie.assignment.domain.vo.AssignmentType;

import java.time.Instant;
import com.lumie.common.util.Timestamps;
import java.util.List;

public record AssignmentResponse(
    Long id,
    Long classId,
    String title,
    String description,
    AssignmentType type,
    Instant dueDate,
    Integer maxScore,
    List<Long> attachments,
    AssignmentStatus status,
    Instant createdAt,
    Instant updatedAt
) {
    public static AssignmentResponse from(Assignment entity) {
        return new AssignmentResponse(
            entity.getId(),
            entity.getClassId(),
            entity.getTitle(),
            entity.getDescription(),
            entity.getType(),
            Timestamps.utcInstant(entity.getDueDate()),
            entity.getMaxScore(),
            entity.getAttachments(),
            entity.getStatus(),
            Timestamps.utcInstant(entity.getCreatedAt()),
            Timestamps.utcInstant(entity.getUpdatedAt())
        );
    }
}
