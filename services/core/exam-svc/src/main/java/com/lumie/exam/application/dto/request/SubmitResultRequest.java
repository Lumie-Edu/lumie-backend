package com.lumie.exam.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Map;

public record SubmitResultRequest(
        @NotNull(message = "Student ID is required")
        @Positive(message = "Student ID must be positive")
        Long studentId,

        @NotNull(message = "Answers are required")
        Map<String, String> answers
) {
}
