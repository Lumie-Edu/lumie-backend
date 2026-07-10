package com.lumie.assignment.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record AssignmentQuestionResultResponse(
    Integer questionNumber,
    @Schema(nullable = true) String selectedChoice,
    @Schema(nullable = true) Boolean correct,
    @Schema(nullable = true) String questionType,
    @Schema(nullable = true) Integer score,
    @Schema(nullable = true) String correctAnswer
) {
}
