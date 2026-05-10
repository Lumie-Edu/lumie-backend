package com.lumie.exam.application.dto.response;

import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.vo.ExamCategory;

import java.time.Instant;
import com.lumie.common.util.Timestamps;
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
        Instant createdAt,
        Instant updatedAt
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
                Timestamps.utcInstant(exam.getCreatedAt()),
                Timestamps.utcInstant(exam.getUpdatedAt())
        );
    }
}
