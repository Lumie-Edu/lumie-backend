package com.lumie.exam.application.dto.response.statistics;

import com.lumie.exam.domain.vo.ExamCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

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
        boolean isPassed,
        List<StudentClassSummary> classes
) {
    public record StudentClassSummary(
            Long classId,
            String className
    ) {
    }
}
