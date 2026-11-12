package com.lumie.exam.application.dto.response;

import java.util.Map;

public record ExamAnswerRegradePreviewResponse(
        Long examId,
        long examVersion,
        String previewFingerprint,
        boolean noOp,
        Map<String, AnswerChange> changedAnswers,
        int regradedResultCount,
        int skippedManualOverrideCount,
        int updatedSubmissionCount,
        Map<Integer, Long> scoreImpact,
        Map<String, Long> gradeImpact
) {
    public record AnswerChange(String currentAnswer, String proposedAnswer) {}
}
