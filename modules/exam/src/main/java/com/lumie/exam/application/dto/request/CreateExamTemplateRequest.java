package com.lumie.exam.application.dto.request;

import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Map;

public record CreateExamTemplateRequest(
        @NotBlank(message = "Template name is required")
        String name,

        ExamCategory category,

        GradingType gradingType,

        GradeScale gradeScale,

        @NotNull(message = "Total questions is required")
        @Positive(message = "Total questions must be positive")
        Integer totalQuestions,

        @NotNull(message = "Question scores are required")
        Map<String, Integer> questionScores,

        Map<String, String> questionTypes,

        Integer passScore
) {
}
