package com.lumie.exam.application.service;

import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.entity.QuestionResult;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradingType;
import com.lumie.exam.application.dto.response.statistics.StudentRankResponse;
import com.lumie.exam.application.dto.response.statistics.StabilityIndexResponse;
import java.util.*;

public final class ExamStatisticsProjection {
    private ExamStatisticsProjection() {}

    public static Map<String, List<Double>> calculateTypeAccuracies(Exam exam, List<ExamResult> examResults) {
        if (exam.getQuestionTypes() == null) {
            return Map.of();
        }
        Map<String, List<Double>> typeAccuracies = new HashMap<>();
        for (ExamResult result : examResults) {
            Map<String, int[]> resultTypeStats = calculateTypeStats(exam, result.getQuestionResults());
            for (Map.Entry<String, int[]> entry : resultTypeStats.entrySet()) {
                int correct = entry.getValue()[0];
                int total = entry.getValue()[1];
                double accuracy = total > 0 ? (double) correct / total * 100 : 0;
                typeAccuracies.computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>()).add(accuracy);
            }
        }
        return typeAccuracies;
    }

    public static List<StudentRankResponse.TypePercentile> calculateTypePercentiles(
            Exam exam,
            ExamResult result,
            Map<String, List<Double>> typeAccuracies
    ) {
        if (exam.getQuestionTypes() == null) {
            return Collections.emptyList();
        }

        Map<String, int[]> studentTypeStats = calculateTypeStats(exam, result.getQuestionResults());
        return studentTypeStats.entrySet().stream()
                .map(entry -> {
                    String type = entry.getKey();
                    int correct = entry.getValue()[0];
                    int total = entry.getValue()[1];
                    double accuracy = total > 0 ? (double) correct / total * 100 : 0;
                    List<Double> allAccuracies = typeAccuracies.getOrDefault(type, Collections.emptyList());
                    long lowerCount = allAccuracies.stream().filter(a -> a < accuracy).count();
                    double percentile = allAccuracies.isEmpty()
                            ? 0
                            : (double) lowerCount / allAccuracies.size() * 100;

                    return new StudentRankResponse.TypePercentile(
                            type,
                            correct,
                            total,
                            Math.round(accuracy * 10) / 10.0,
                            Math.round(percentile * 10) / 10.0
                    );
                })
                .toList();
    }

    public static Map<String, int[]> calculateTypeStats(Exam exam, Collection<QuestionResult> questionResults) {
        Map<String, int[]> typeStats = new HashMap<>();
        for (QuestionResult qr : questionResults) {
            String type = exam.getQuestionType(qr.getQuestionNumber());
            if (type == null) {
                continue;
            }
            typeStats.computeIfAbsent(type, ignored -> new int[]{0, 0});
            typeStats.get(type)[1]++;
            if (qr.isCorrect()) {
                typeStats.get(type)[0]++;
            }
        }
        return typeStats;
    }

    public static StabilityIndexResponse calculateStabilityIndex(
            Long studentId,
            List<ExamResult> results,
            StatisticsCalculator statisticsCalculator,
            GradeCalculator gradeCalculator,
            Map<Long, List<Integer>> relativeScoresByExamId) {
        if (results.isEmpty()) {
            return new StabilityIndexResponse(studentId, 0, 0.0, 0.0, 0.0, "N/A", List.of());
        }

        List<Integer> scores = results.stream()
                .map(ExamResult::getTotalScore)
                .toList();
        double average = statisticsCalculator.calculateMean(scores);
        double stdDev = statisticsCalculator.calculateStandardDeviation(scores);
        double cv = statisticsCalculator.calculateCoefficientOfVariation(scores);

        List<StabilityIndexResponse.ScoreHistory> history = results.stream()
                .sorted(Comparator.comparing(ExamResult::getCreatedAt))
                .map(r -> new StabilityIndexResponse.ScoreHistory(
                        r.getExam().getId(),
                        r.getExam().getName(),
                        r.getTotalScore(),
                        r.getExam().calculateTotalPossibleScore(),
                        gradeCalculator.resolveStoredOrRelativeGrade(r.getExam(), r,
                                r.getExam().getCategory() == ExamCategory.GRADED
                                        && r.getExam().getGradingType() == GradingType.RELATIVE
                                        ? Objects.requireNonNull(relativeScoresByExamId.get(r.getExam().getId()),
                                                "Missing full relative-grade cohort for exam " + r.getExam().getId())
                                        : List.of()),
                        r.passStatus(),
                        r.getExam().getCategory(),
                        r.getCreatedAt()
                ))
                .toList();

        return new StabilityIndexResponse(
                studentId,
                results.size(),
                Math.round(average * 10) / 10.0,
                Math.round(stdDev * 10) / 10.0,
                Math.round(cv * 10) / 10.0,
                statisticsCalculator.determineStabilityLevel(cv),
                history
        );
    }
}
