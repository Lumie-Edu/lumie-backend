package com.lumie.exam.application.dto.response.statistics;

import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;

import java.util.List;

public record ExamStatisticsResponse(
        Long examId,
        String examName,
        ExamCategory examCategory,
        GradingType gradingType,
        GradeScale gradeScale,
        int participantCount,
        double average,
        int highest,
        int lowest,
        double standardDeviation,
        // P/NP 시험 전용 필드
        Double passRate,
        Integer passCount,
        Integer failCount,
        // 등급제 시험 전용 필드
        List<GradeDistribution> gradeDistribution,
        List<ScoreRangeDistribution> scoreRangeDistribution,
        List<TypeAccuracy> typeAccuracyList,
        List<TopIncorrectQuestion> topIncorrectQuestions
) {
    public record GradeDistribution(
            int grade,
            int count,
            double percentage,
            Integer cutoffScore  // 해당 등급의 커트라인 점수
    ) {}

    public record ScoreRangeDistribution(
            String range,
            int count,
            double percentage
    ) {}

    public record TypeAccuracy(
            String questionType,
            int totalQuestions,
            int correctCount,
            double accuracyRate
    ) {}

    public record TopIncorrectQuestion(
            int questionNumber,
            String questionType,
            String correctAnswer,
            int totalAttempts,
            int incorrectCount,
            double incorrectRate,
            String topWrongChoice,
            double topWrongChoiceRate
    ) {}
}
