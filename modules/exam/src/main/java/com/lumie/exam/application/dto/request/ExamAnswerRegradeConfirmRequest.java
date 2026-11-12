package com.lumie.exam.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record ExamAnswerRegradeConfirmRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull Map<String, String> answerChanges,
        @NotNull Map<String, Integer> questionScores,
        @NotNull Map<String, String> questionTypes
) {
}
