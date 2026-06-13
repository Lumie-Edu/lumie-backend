package com.lumie.assignment.application.dto.response;

import com.lumie.assignment.domain.entity.AssignmentSubmission;
import com.lumie.assignment.domain.vo.SubmissionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record SubmissionResponse(
    Long id,
    Long assignmentId,
    @Schema(nullable = true)
    Long studentId,
    @Schema(nullable = true) String content,
    @Schema(nullable = true) List<Long> attachments,
    @Schema(nullable = true) Integer score,
    @Schema(nullable = true) String feedback,
    @Schema(nullable = true) Instant submittedAt,
    @Schema(nullable = true) Instant gradedAt,
    SubmissionStatus status,
    Instant createdAt,
    Instant updatedAt
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
