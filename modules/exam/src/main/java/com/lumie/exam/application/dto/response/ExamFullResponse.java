package com.lumie.exam.application.dto.response;

import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.vo.ExamCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

public record ExamFullResponse(
        Long id,
        String name,
        ExamCategory category,
        Integer totalQuestions,
        Map<String, String> correctAnswers,
        Map<String, Integer> questionScores,
        @Schema(nullable = true) Map<String, String> questionTypes,
        Integer totalPossibleScore,
        @Schema(nullable = true) Integer passScore,
        Instant createdAt,
        Instant updatedAt
) {
    public static ExamFullResponse from(Exam exam) {
        return new ExamFullResponse(
                exam.getId(),
                exam.getName(),
                exam.getCategory(),
                exam.getTotalQuestions(),
                exam.getCorrectAnswers(),
                exam.getQuestionScores(),
                exam.getQuestionTypes(),
                exam.calculateTotalPossibleScore(),
                exam.getPassScore(),
                exam.getCreatedAt(),
                exam.getUpdatedAt()
        );
    }
}
