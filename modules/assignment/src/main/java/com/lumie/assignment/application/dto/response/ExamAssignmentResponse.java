package com.lumie.assignment.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ExamAssignmentResponse(
    Long id,
    String name,
    String category,
    String gradingType,
    @Schema(nullable = true)
    String gradeScale,
    int totalQuestions,
    int totalPossibleScore,
    @Schema(nullable = true) Integer passScore
) {
}
