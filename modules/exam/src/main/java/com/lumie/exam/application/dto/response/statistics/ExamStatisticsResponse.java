package com.lumie.exam.application.dto.response.statistics;

import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ExamStatisticsResponse(
        Long examId,
        String examName,
        ExamCategory examCategory,
        @Schema(nullable = true) GradingType gradingType,   // null for PASS_FAIL
        @Schema(nullable = true) GradeScale gradeScale,     // null for PASS_FAIL
        int participantCount,
        double average,
        int highest,
        int lowest,
        double standardDeviation,
        // P/NP 시험 전용 필드 (등급제 시험에서는 null)
        @Schema(nullable = true) Double passRate,
        @Schema(nullable = true) Integer passCount,
        @Schema(nullable = true) Integer failCount,
        // 등급제 시험 전용 필드 (PASS_FAIL 시험에서는 빈 배열)
        @Schema(description = "등급별 분포. PASS_FAIL 시험에서는 빈 배열.") List<GradeDistribution> gradeDistribution,
        @Schema(description = "점수 구간별 분포. PASS_FAIL 시험에서는 빈 배열.") List<ScoreRangeDistribution> scoreRangeDistribution,
        // 공통 필드 — 채점 데이터가 없으면 빈 배열
        @Schema(description = "문항 유형별 정답률 목록. 채점 데이터가 없으면 빈 배열.") List<TypeAccuracy> typeAccuracyList,
        @Schema(description = "오답률 상위 문항 목록. 채점 데이터가 없으면 빈 배열.") List<TopIncorrectQuestion> topIncorrectQuestions
) {
    public record GradeDistribution(
            int grade,
            int count,
            double percentage,
            @Schema(nullable = true) Integer cutoffScore  // 해당 등급의 커트라인 점수 (없으면 null)
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
