package com.lumie.assignment.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record StudentAssignmentDetailResponse(
    AssignmentResponse assignment,
    boolean canSubmit,
    @Schema(nullable = true) SubmissionResponse submission,
    @Schema(nullable = true) ExamAssignmentResponse exam,
    List<AssignmentQuestionResultResponse> questionResults
) {
}
