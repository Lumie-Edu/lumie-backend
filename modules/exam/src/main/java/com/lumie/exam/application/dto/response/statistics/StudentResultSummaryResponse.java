package com.lumie.exam.application.dto.response.statistics;

import com.lumie.exam.domain.vo.ExamCategory;
import io.swagger.v3.oas.annotations.media.Schema;

public record StudentResultSummaryResponse(
        Long resultId,
        @Schema(nullable = true)
        Long studentId,
        @Schema(nullable = true) String phoneNumber,
        boolean isRegistered,
        String studentName,
        boolean isActive,
        int score,
        @Schema(nullable = true) Integer grade,
        ExamCategory examCategory,
        boolean isPassed
) {
}
