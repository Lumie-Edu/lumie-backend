package com.lumie.exam.application.dto.response;

import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.QuestionResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record QuestionResultResponse(
        Long id,
        Integer questionNumber,
        @Schema(nullable = true) String selectedChoice,
        String correctAnswer,
        Boolean isCorrect,
        Integer score,
        Integer earnedScore,
        @Schema(nullable = true) String questionType
) {
    public static QuestionResultResponse from(QuestionResult result, Exam exam) {
        return new QuestionResultResponse(
                result.getId(),
                result.getQuestionNumber(),
                result.getSelectedChoice(),
                exam.getCorrectAnswerForQuestion(result.getQuestionNumber()),
                result.getIsCorrect(),
                result.getScore(),
                result.getEarnedScore(),
                exam.getQuestionType(result.getQuestionNumber())
        );
    }
}
