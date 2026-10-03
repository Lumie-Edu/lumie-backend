package com.lumie.exam.application.dto.response.statistics;

import com.lumie.exam.domain.vo.ExamCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record StudentGradeResponse(
        @Schema(nullable = true) Long studentId,
        @Schema(nullable = true) String phoneNumber,
        boolean isRegistered,
        String studentName,
        int score,
        int rank,
        double percentile,
        @Schema(nullable = true) Integer grade,
        ExamCategory examCategory,
        @Schema(nullable = true) Boolean isPassed,
        Instant submittedAt,
        Long resultId
) {}
