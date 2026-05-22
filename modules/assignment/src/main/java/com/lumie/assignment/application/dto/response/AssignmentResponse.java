package com.lumie.assignment.application.dto.response;

import com.lumie.assignment.domain.entity.Assignment;
import com.lumie.assignment.domain.vo.AssignmentStatus;
import com.lumie.assignment.domain.vo.AssignmentType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record AssignmentResponse(
    Long id,
    Long classId,
    String title,
    @Schema(nullable = true) String description,
    AssignmentType type,
    @Schema(nullable = true) Instant dueDate,
    @Schema(nullable = true) Integer maxScore,
    @Schema(nullable = true) List<Long> attachments,
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
            entity.getDueDate(),
            entity.getMaxScore(),
            entity.getAttachments(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
