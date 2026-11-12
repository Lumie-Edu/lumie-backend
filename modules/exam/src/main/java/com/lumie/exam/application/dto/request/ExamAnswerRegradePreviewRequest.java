package com.lumie.exam.application.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record ExamAnswerRegradePreviewRequest(
        @NotNull Map<String, String> answerChanges
) {
}
