package com.lumie.assignment.application.dto;

import com.lumie.assignment.domain.entity.AssignmentSubmission;
import com.lumie.assignment.domain.vo.SubmissionStatus;

import java.time.Instant;
import com.lumie.common.util.Timestamps;
import java.util.List;

public record SubmissionResponse(
    Long id,
    Long assignmentId,
    Long studentId,
    String content,
    List<Long> attachments,
    Integer score,
    String feedback,
    Instant submittedAt,
    Instant gradedAt,
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
            Timestamps.utcInstant(entity.getSubmittedAt()),
            Timestamps.utcInstant(entity.getGradedAt()),
            entity.getStatus(),
            Timestamps.utcInstant(entity.getCreatedAt()),
            Timestamps.utcInstant(entity.getUpdatedAt())
        );
    }
}
