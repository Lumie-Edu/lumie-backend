package com.lumie.assignment.application.dto.response;

import com.lumie.assignment.domain.entity.Assignment;
import com.lumie.assignment.domain.entity.AssignmentTarget;
import com.lumie.assignment.domain.vo.AssignmentTargetType;
import com.lumie.assignment.domain.vo.AssignmentStatus;
import com.lumie.assignment.domain.vo.EvaluationMode;
import com.lumie.assignment.domain.vo.SubmissionMode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record AssignmentResponse(
    Long id,
    @Schema(nullable = true)
    Long classId,
    @Schema(nullable = true) Instant dueDate,
    @Schema(nullable = true) Integer maxScore,
    @Schema(nullable = true) List<Long> attachments,
    AssignmentStatus status,
    SubmissionMode submissionMode,
    EvaluationMode evaluationMode,
    @Schema(nullable = true) Long linkedExamId,
    @Schema(nullable = true) String examName,
    List<Long> targetClassIds,
    List<Long> targetStudentIds,
    boolean showScore,
    boolean showPassFail,
    boolean showGrade,
    boolean showQuestionResults,
    boolean showCorrectAnswers,
    Instant createdAt,
    Instant updatedAt
) {
    public AssignmentResponse(Long id, Long classId, Instant dueDate, Integer maxScore,
                              List<Long> attachments, AssignmentStatus status,
                              Instant createdAt, Instant updatedAt) {
        this(id, classId, dueDate, maxScore, attachments, status,
                SubmissionMode.EXAM_MANUAL, EvaluationMode.EXAM_AUTO, null, null, List.of(), List.of(),
                true, true, false, true, false, createdAt, updatedAt);
    }

    public static AssignmentResponse from(Assignment entity) {
        return from(entity, List.of());
    }

    public static AssignmentResponse from(Assignment entity, List<AssignmentTarget> targets) {
        return from(entity, targets, null);
    }

    public static AssignmentResponse from(Assignment entity, List<AssignmentTarget> targets, String examName) {
        return new AssignmentResponse(
            entity.getId(),
            entity.getClassId(),
            entity.getDueDate(),
            entity.getMaxScore(),
            entity.getAttachments(),
            entity.getStatus(),
            entity.getSubmissionMode(),
            entity.getEvaluationMode(),
            entity.getLinkedExamId(),
            examName,
            targets.stream()
                    .filter(target -> target.getTargetType() == AssignmentTargetType.CLASS)
                    .map(AssignmentTarget::getTargetId)
                    .toList(),
            targets.stream()
                    .filter(target -> target.getTargetType() == AssignmentTargetType.STUDENT)
                    .map(AssignmentTarget::getTargetId)
                    .toList(),
            entity.isShowScore(),
            entity.isShowPassFail(),
            entity.isShowGrade(),
            entity.isShowQuestionResults(),
            entity.isShowCorrectAnswers(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
