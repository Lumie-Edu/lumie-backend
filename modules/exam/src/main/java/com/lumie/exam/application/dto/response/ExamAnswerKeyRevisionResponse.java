package com.lumie.exam.application.dto.response;

import com.lumie.exam.domain.entity.ExamAnswerKeyRevision;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

public record ExamAnswerKeyRevisionResponse(
        Long id,
        Long actorUserId,
        Instant createdAt,
        Map<String, AnswerKeyChange> changedAnswers,
        Map<String, AnswerKeyChange> changedFields,
        int regradedResultCount,
        int skippedManualOverrideCount,
        int updatedSubmissionCount
) {
    public static ExamAnswerKeyRevisionResponse from(ExamAnswerKeyRevision revision) {
        Map<String, AnswerKeyChange> changed = revision.getChangedAnswers().entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> new AnswerKeyChange(entry.getValue().get("before"), entry.getValue().get("after"))));
        Map<String, AnswerKeyChange> fields = revision.getChangedFields().entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> new AnswerKeyChange(entry.getValue().get("before"), entry.getValue().get("after"))));
        return new ExamAnswerKeyRevisionResponse(
                revision.getId(), revision.getActorUserId(), revision.getCreatedAt(),
                changed, fields, revision.getRegradedResultCount(), revision.getSkippedManualOverrideCount(),
                revision.getUpdatedSubmissionCount());
    }

    public record AnswerKeyChange(String before, String after) {}
}
