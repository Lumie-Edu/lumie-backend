package com.lumie.assignment.application.dto.response;

import com.lumie.assignment.domain.entity.Assignment;
import com.lumie.assignment.application.port.out.AssignmentAudienceReadPort.Targets;
import com.lumie.assignment.domain.vo.AssignmentStatus;
import com.lumie.assignment.domain.vo.EvaluationMode;
import com.lumie.assignment.domain.vo.SubmissionMode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record AssignmentResponse(
    Long id,
    Instant dueDate,
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
    String rosterAccuracy,
    Instant createdAt,
    Instant updatedAt
) {
    public static AssignmentResponse from(Assignment entity) {
        return from(entity, new Targets(List.of(), List.of()));
    }

    public static AssignmentResponse from(Assignment entity, Targets targets) {
        return from(entity, targets, null);
    }

    public static AssignmentResponse from(Assignment entity, Targets targets, String examName) {
        return from(entity, targets, examName, "UNVERIFIED");
    }

    public static AssignmentResponse from(Assignment entity, Targets targets,
                                          String examName, String rosterAccuracy) {
        return new AssignmentResponse(
            entity.getId(),
            entity.getDueDate(),
            entity.getMaxScore(),
            entity.getAttachments(),
            entity.getStatus(),
            entity.getSubmissionMode(),
            entity.getEvaluationMode(),
            entity.getLinkedExamId(),
            examName,
            targets.classIds(),
            targets.studentIds(),
            entity.isShowScore(),
            entity.isShowPassFail(),
            entity.isShowGrade(),
            entity.isShowQuestionResults(),
            entity.isShowCorrectAnswers(),
            rosterAccuracy,
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
