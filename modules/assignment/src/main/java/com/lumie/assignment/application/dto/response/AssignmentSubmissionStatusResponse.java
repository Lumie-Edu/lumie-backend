package com.lumie.assignment.application.dto.response;

import com.lumie.assignment.domain.vo.SubmissionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record AssignmentSubmissionStatusResponse(
    Long studentId,
    @Schema(nullable = true) String studentName,
    SubmissionStatus status,
    @Schema(nullable = true) SubmissionResponse submission
) {
}
