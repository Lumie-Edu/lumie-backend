package com.lumie.exam.application.dto.response;

import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record ExamResponse(
        Long id,
        String name,
        ExamCategory category,
        @Schema(nullable = true) GradingType gradingType,
        @Schema(nullable = true) GradeScale gradeScale,
        Integer totalQuestions,
        Integer totalPossibleScore,
        @Schema(nullable = true) Integer passScore,
        Instant createdAt,
        Instant updatedAt
) {
    public static ExamResponse from(Exam exam) {
        return new ExamResponse(
                exam.getId(),
                exam.getName(),
                exam.getCategory(),
                exam.isPassFail() ? null : exam.getGradingType(),
                exam.isPassFail() ? null : exam.getGradeScale(),
                exam.getTotalQuestions(),
                exam.calculateTotalPossibleScore(),
                exam.isPassFail() ? exam.getPassScore() : null,
                exam.getCreatedAt(),
                exam.getUpdatedAt()
        );
    }
}
