package com.lumie.assignment.application.dto.response;

import com.lumie.assignment.domain.vo.SubmissionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record AssignmentSubmissionStatusResponse(
    Long studentId,
    SubmissionStatus status,
    @Schema(nullable = true) SubmissionResponse submission
) {
}
