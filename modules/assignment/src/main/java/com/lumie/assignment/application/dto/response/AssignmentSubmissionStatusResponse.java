package com.lumie.assignment.application.dto.response;

import com.lumie.assignment.domain.vo.SubmissionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record AssignmentSubmissionStatusResponse(
    Long recipientId,
    @Schema(nullable = true)
    Long studentId,
    @Schema(nullable = true) String studentName,
    SubmissionStatus status,
    @Schema(nullable = true) SubmissionResponse submission
) {
    public AssignmentSubmissionStatusResponse(Long studentId, String studentName,
                                              SubmissionStatus status, SubmissionResponse submission) {
        this(null, studentId, studentName, status, submission);
    }
}
