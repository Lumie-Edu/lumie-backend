package com.lumie.exam.application.port.out;

import java.util.List;
import java.util.Map;

public interface OmrServicePort {

    OmrGradingResult gradeOmrImage(byte[] imageData,
                                    Map<String, String> correctAnswers,
                                    Map<String, Integer> questionScores,
                                    Map<String, String> questionTypes);

    record OmrGradingResult(
            int totalScore,
            int grade,
            String phoneNumber,
            List<OmrQuestionResult> results
    ) {
    }

    record OmrQuestionResult(
            int questionNumber,
            String studentAnswer,
            String correctAnswer,
            int score,
            int earnedScore,
            String questionType
    ) {
    }
}
