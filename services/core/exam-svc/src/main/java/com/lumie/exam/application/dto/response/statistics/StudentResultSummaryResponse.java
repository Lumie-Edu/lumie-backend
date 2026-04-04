package com.lumie.exam.application.dto.response.statistics;

import com.lumie.exam.domain.vo.ExamCategory;

public record StudentResultSummaryResponse(
        Long resultId,
        Long studentId,
        String phoneNumber,
        boolean isRegistered,
        String studentName,
        String academyName,
        boolean isActive,
        int score,
        Integer grade,
        ExamCategory examCategory,
        boolean isPassed
) {
}
