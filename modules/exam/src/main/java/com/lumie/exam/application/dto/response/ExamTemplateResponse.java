package com.lumie.exam.application.dto.response;

import com.lumie.exam.domain.entity.ExamTemplate;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

public record ExamTemplateResponse(
        Long id,
        String name,
        ExamCategory category,
        @Schema(nullable = true) GradingType gradingType,
        @Schema(nullable = true) GradeScale gradeScale,
        Integer totalQuestions,
        Integer totalPossibleScore,
        Map<String, Integer> questionScores,
        @Schema(nullable = true) Map<String, String> questionTypes,
        @Schema(nullable = true) Integer passScore,
        Instant createdAt,
        Instant updatedAt
) {
    public static ExamTemplateResponse from(ExamTemplate template) {
        return new ExamTemplateResponse(
                template.getId(),
                template.getName(),
                template.getCategory(),
                template.getGradingType(),
                template.getGradeScale(),
                template.getTotalQuestions(),
                template.calculateTotalPossibleScore(),
                template.getQuestionScores(),
                template.getQuestionTypes(),
                template.getPassScore(),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }
}
