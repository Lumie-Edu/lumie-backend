package com.lumie.assignment.application.dto;

import com.lumie.assignment.domain.entity.Assignment;
import com.lumie.assignment.domain.vo.AssignmentStatus;
import com.lumie.assignment.domain.vo.AssignmentType;

import java.time.LocalDateTime;
import java.util.List;

public record AssignmentResponse(
    Long id,
    Long classId,
    String title,
    String description,
    AssignmentType type,
    LocalDateTime dueDate,
    Integer maxScore,
    List<Long> attachments,
    AssignmentStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
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
