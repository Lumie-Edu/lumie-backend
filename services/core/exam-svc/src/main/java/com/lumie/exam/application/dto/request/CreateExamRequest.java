package com.lumie.exam.application.dto.request;

import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Map;

public record CreateExamRequest(
        @NotBlank(message = "Exam name is required")
        String name,

        ExamCategory category,

        GradingType gradingType,

        GradeScale gradeScale,  // 9등급제 또는 5등급제

        @NotNull(message = "Total questions is required")
        @Positive(message = "Total questions must be positive")
        Integer totalQuestions,

        @NotNull(message = "Correct answers are required")
        Map<String, String> correctAnswers,

        @NotNull(message = "Question scores are required")
        Map<String, Integer> questionScores,

        Map<String, String> questionTypes,

        Integer passScore
) {
}
