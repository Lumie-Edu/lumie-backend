package com.lumie.assignment.application.dto.response;

import com.lumie.assignment.domain.entity.AssignmentSubmission;
import com.lumie.assignment.domain.exception.AssignmentErrorCode;
import com.lumie.common.exception.BusinessException;
import com.lumie.exam.api.ExamService.AssignmentResultData;
import com.lumie.assignment.domain.vo.SubmissionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record SubmissionResponse(
    Long id,
    Long assignmentId,
    @Schema(nullable = true)
    Long studentId,
    @Schema(nullable = true) String content,
    @Schema(nullable = true) List<Long> attachments,
    @Schema(nullable = true) Map<String, String> answers,
    @Schema(nullable = true) Long examResultId,
    @Schema(nullable = true) Integer score,
    @Schema(nullable = true) Boolean passed,
    @Schema(nullable = true) String gradeValue,
    @Schema(nullable = true) String feedback,
    @Schema(nullable = true) Instant submittedAt,
    @Schema(nullable = true) Instant gradedAt,
    SubmissionStatus status,
    Instant createdAt,
    Instant updatedAt
) {
    public SubmissionResponse(Long id, Long assignmentId, Long studentId, String content,
                              List<Long> attachments, Integer score, String feedback,
                              Instant submittedAt, Instant gradedAt, SubmissionStatus status,
                              Instant createdAt, Instant updatedAt) {
        this(id, assignmentId, studentId, content, attachments, null, null, score,
                null, null, feedback, submittedAt, gradedAt, status, createdAt, updatedAt);
    }

    public static SubmissionResponse from(AssignmentSubmission entity) {
        return new SubmissionResponse(
            entity.getId(),
            entity.getAssignmentId(),
            entity.getStudentId(),
            entity.getContent(),
            entity.getAttachments(),
            entity.getAnswers(),
            entity.getExamResultId(),
            entity.getScore(),
            entity.getPassed(),
            entity.getGradeValue(),
            entity.getFeedback(),
            entity.getSubmittedAt(),
            entity.getGradedAt(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public static SubmissionResponse from(AssignmentSubmission entity, AssignmentResultData result) {
        if (result == null || !Objects.equals(entity.getStudentId(), result.studentId())) {
            throw new BusinessException(
                    AssignmentErrorCode.INVALID_SUBMISSION,
                    "Assignment result is missing or belongs to another student");
        }
        return new SubmissionResponse(entity.getId(), entity.getAssignmentId(), entity.getStudentId(),
                entity.getContent(), entity.getAttachments(), result.answers(), entity.getExamResultId(),
                result.score(), result.passed(), result.grade() == null ? null : String.valueOf(result.grade()),
                entity.getFeedback(), entity.getSubmittedAt(), entity.getGradedAt(), entity.getStatus(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
