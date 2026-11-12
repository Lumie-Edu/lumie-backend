package com.lumie.exam.application.dto.response;

import com.lumie.exam.domain.entity.ExamAnswerKeyRevision;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

public record ExamAnswerRegradeRevisionResponse(
        @Schema(nullable = true)
        Long revisionId,
        Long examId,
        long examVersion,
        @Schema(nullable = true)
        Instant createdAt,
        @Schema(nullable = true)
        Long actorUserId,
        Map<String, ChangedAnswer> changedAnswers,
        Map<String, ChangedAnswer> changedFields,
        int regradedResultCount,
        int skippedManualOverrideCount,
        int updatedSubmissionCount,
        boolean noOp
) {
    public static ExamAnswerRegradeRevisionResponse from(ExamAnswerKeyRevision revision) {
        Map<String, ChangedAnswer> changed = revision.getChangedAnswers().entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> new ChangedAnswer(entry.getValue().get("before"), entry.getValue().get("after"))));
        Map<String, ChangedAnswer> fields = revision.getChangedFields().entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> new ChangedAnswer(entry.getValue().get("before"), entry.getValue().get("after"))));
        return new ExamAnswerRegradeRevisionResponse(
                revision.getId(),
                revision.getExam().getId(),
                revision.getExamVersionAfter(),
                revision.getCreatedAt(),
                revision.getActorUserId(),
                changed,
                fields,
                revision.getRegradedResultCount(),
                revision.getSkippedManualOverrideCount(),
                revision.getUpdatedSubmissionCount(),
                false);
    }

    public static ExamAnswerRegradeRevisionResponse noOp(
            Long examId, long examVersion, Map<String, ChangedAnswer> changedAnswers) {
        return new ExamAnswerRegradeRevisionResponse(
                null, examId, examVersion, null, null, changedAnswers, Map.of(), 0, 0, 0, true);
    }

    public record ChangedAnswer(String currentAnswer, String proposedAnswer) {}
}
