package com.lumie.assignment.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Map;

public record CreateAssignmentExamRequest(
        @NotBlank(message = "Exam name is required")
        String name,
        String category,
        String gradingType,
        String gradeScale,
        @NotNull(message = "Total questions is required")
        @Positive(message = "Total questions must be positive")
        Integer totalQuestions,
        @NotNull(message = "Correct answers are required")
        Map<String, String> correctAnswers,
        @NotNull(message = "Question scores are required")
        Map<String, Integer> questionScores,
        Map<String, String> questionTypes,
        Integer passScore
) {
}
