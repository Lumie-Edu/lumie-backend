package com.lumie.assignment.application.dto;

import com.lumie.assignment.domain.entity.AssignmentSubmission;
import com.lumie.assignment.domain.vo.SubmissionStatus;

import java.time.LocalDateTime;
import java.util.List;

public record SubmissionResponse(
    Long id,
    Long assignmentId,
    Long studentId,
    String content,
    List<Long> attachments,
    Integer score,
    String feedback,
    LocalDateTime submittedAt,
    LocalDateTime gradedAt,
    SubmissionStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static SubmissionResponse from(AssignmentSubmission entity) {
        return new SubmissionResponse(
            entity.getId(),
            entity.getAssignmentId(),
            entity.getStudentId(),
            entity.getContent(),
            entity.getAttachments(),
            entity.getScore(),
            entity.getFeedback(),
            entity.getSubmittedAt(),
            entity.getGradedAt(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
