package com.lumie.assignment.application.dto.response;

import com.lumie.assignment.domain.vo.SubmissionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record AssignmentStudentGradeResponse(
        Long studentId,
        @Schema(nullable = true) String phoneNumber,
        boolean isRegistered,
        String studentName,
        @Schema(nullable = true) Integer score,
        @Schema(nullable = true) Integer rank,
        @Schema(nullable = true) Double percentile,
        @Schema(nullable = true) Integer grade,
        String examCategory,
        @Schema(nullable = true) Boolean isPassed,
        @Schema(nullable = true) Instant submittedAt,
        @Schema(nullable = true) Long resultId,
        SubmissionStatus submissionStatus
) {}
