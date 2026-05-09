package com.lumie.exam.application.dto.response;

import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.vo.ExamCategory;

import java.time.LocalDateTime;
import java.util.Map;

public record ExamDetailResponse(
        Long id,
        String name,
        ExamCategory category,
        Integer totalQuestions,
        Map<String, String> correctAnswers,
        Map<String, Integer> questionScores,
        Map<String, String> questionTypes,
        Integer totalPossibleScore,
        Integer passScore,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ExamDetailResponse from(Exam exam) {
        return new ExamDetailResponse(
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
