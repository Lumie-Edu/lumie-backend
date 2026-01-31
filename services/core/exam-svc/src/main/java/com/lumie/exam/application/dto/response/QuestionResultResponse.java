package com.lumie.exam.application.dto.response;

import com.lumie.exam.domain.entity.QuestionResult;

public record QuestionResultResponse(
        Long id,
        Integer questionNumber,
        String selectedChoice,
        Boolean isCorrect,
        Integer score,
        Integer earnedScore
) {
    public static QuestionResultResponse from(QuestionResult result) {
        return new QuestionResultResponse(
                result.getId(),
                result.getQuestionNumber(),
                result.getSelectedChoice(),
                result.getIsCorrect(),
                result.getScore(),
                result.getEarnedScore()
        );
    }
}
