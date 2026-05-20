package com.lumie.exam.application.service;

import com.lumie.exam.application.dto.request.GoalSimulationRequest;
import com.lumie.exam.application.dto.response.statistics.*;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.entity.QuestionResult;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.exception.ExamException;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.application.service.GradeCalculator;
import com.lumie.exam.application.service.StatisticsCalculator;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StudentAnalyticsQueryService {

    private final ExamResultRepository resultRepository;
    private final StatisticsCalculator statisticsCalculator;
    private final GradeCalculator gradeCalculator;

    private static final int[] GRADE_PERCENTILE_THRESHOLDS = {4, 11, 23, 40, 60, 77, 89, 96, 100};

    public StudentRankResponse getStudentRank(Long studentId, Long examId) {
        log.debug("Getting rank for student: {} in exam: {}", studentId, examId);

        ExamResult result = resultRepository.findByExamIdAndStudentId(examId, studentId)
                .orElseThrow(() -> new ExamException(ExamErrorCode.RESULT_NOT_FOUND));

        Exam exam = result.getExam();
        List<ExamResult> allResults = resultRepository.findByExamId(examId);
        List<Integer> allScores = allResults.stream()
                .map(ExamResult::getTotalScore)
                .toList();

        int rank = statisticsCalculator.calculateRank(result.getTotalScore(), allScores);
        double percentile = statisticsCalculator.calculatePercentile(result.getTotalScore(), allScores);

        Integer grade = result.getGrade();
        if (exam.getCategory() == ExamCategory.GRADED &&
            exam.getGradingType() == GradingType.RELATIVE) {
            GradeScale gradeScale = exam.getGradeScale() != null ? exam.getGradeScale() : GradeScale.NINE_GRADE;
            grade = gradeCalculator.calculateRelativeGrade(result.getTotalScore(), allScores, gradeScale);
        }

        List<StudentRankResponse.TypePercentile> typePercentiles = calculateTypePercentiles(exam, result, allResults);

        return new StudentRankResponse(
                studentId, examId, exam.getName(), result.getTotalScore(),
                rank, allResults.size(), Math.round(percentile * 10) / 10.0,
                grade, typePercentiles
        );
    }

    public StabilityIndexResponse getStabilityIndex(Long studentId) {
        log.debug("Getting stability index for student: {}", studentId);

        List<ExamResult> results = resultRepository.findByStudentId(studentId);

        if (results.isEmpty()) {
            return new StabilityIndexResponse(
                    studentId, 0, 0.0, 0.0, 0.0, "N/A", Collections.emptyList()
            );
        }

        List<Integer> scores = results.stream()
                .map(ExamResult::getTotalScore)
                .toList();

        double average = statisticsCalculator.calculateMean(scores);
        double stdDev = statisticsCalculator.calculateStandardDeviation(scores);
        double cv = statisticsCalculator.calculateCoefficientOfVariation(scores);
        String stabilityLevel = statisticsCalculator.determineStabilityLevel(cv);

        List<StabilityIndexResponse.ScoreHistory> history = results.stream()
                .sorted(Comparator.comparing(ExamResult::getCreatedAt))
                .map(r -> new StabilityIndexResponse.ScoreHistory(
                        r.getExam().getId(), r.getExam().getName(),
                        r.getTotalScore(), r.getGrade() != null ? r.getGrade() : 0,
                        r.getCreatedAt()
                ))
                .toList();

        return new StabilityIndexResponse(
                studentId, results.size(),
                Math.round(average * 10) / 10.0,
                Math.round(stdDev * 10) / 10.0,
                Math.round(cv * 10) / 10.0,
                stabilityLevel, history
        );
    }

    public TypeGrowthTrendResponse getTypeGrowthTrend(Long studentId) {
        log.debug("Getting type growth trend for student: {}", studentId);

        List<ExamResult> results = resultRepository.findByStudentId(studentId);

        Map<String, List<TypeGrowthTrendResponse.TrendPoint>> trendsByType = new HashMap<>();

        for (ExamResult result : results) {
            Exam exam = result.getExam();
            if (exam.getQuestionTypes() == null) continue;

            Map<String, int[]> typeStats = new HashMap<>();

            for (QuestionResult qr : result.getQuestionResults()) {
                String type = exam.getQuestionType(qr.getQuestionNumber());
                if (type == null) continue;

                typeStats.computeIfAbsent(type, k -> new int[]{0, 0});
                typeStats.get(type)[1]++;
                if (qr.isCorrect()) {
                    typeStats.get(type)[0]++;
                }
            }

            for (Map.Entry<String, int[]> entry : typeStats.entrySet()) {
                String type = entry.getKey();
                int correct = entry.getValue()[0];
                int total = entry.getValue()[1];
                double accuracy = total > 0 ? (double) correct / total * 100 : 0;

                TypeGrowthTrendResponse.TrendPoint point = new TypeGrowthTrendResponse.TrendPoint(
                        exam.getId(), exam.getName(), result.getCreatedAt(),
                        correct, total, Math.round(accuracy * 10) / 10.0
                );

                trendsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(point);
            }
        }

        List<TypeGrowthTrendResponse.TypeTrend> trends = trendsByType.entrySet().stream()
                .map(entry -> {
                    List<TypeGrowthTrendResponse.TrendPoint> points = entry.getValue().stream()
                            .sorted(Comparator.comparing(TypeGrowthTrendResponse.TrendPoint::examDate))
                            .toList();

                    double growthRate = 0.0;
                    if (points.size() >= 2) {
                        double firstAccuracy = points.get(0).accuracy();
                        double lastAccuracy = points.get(points.size() - 1).accuracy();
                        growthRate = statisticsCalculator.calculateGrowthRate(firstAccuracy, lastAccuracy);
                    }

                    return new TypeGrowthTrendResponse.TypeTrend(
                            entry.getKey(), points, Math.round(growthRate * 10) / 10.0
                    );
                })
                .toList();

        return new TypeGrowthTrendResponse(studentId, trends);
    }

    public NormalizedScoreResponse getNormalizedScores(Long studentId) {
        log.debug("Getting normalized scores for student: {}", studentId);

        List<ExamResult> studentResults = resultRepository.findByStudentId(studentId);

        // Batch-fetch all peer results across the student's exams in one query
        // (was N+1 before — one findByExamId call per exam in the student's history).
        java.util.Set<Long> examIds = studentResults.stream()
                .map(r -> r.getExam().getId())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        java.util.Map<Long, List<Integer>> scoresByExamId = resultRepository.findByExamIdIn(examIds).stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        r -> r.getExam().getId(),
                        java.util.stream.Collectors.mapping(ExamResult::getTotalScore, java.util.stream.Collectors.toList())
                ));

        List<NormalizedScoreResponse.NormalizedExamScore> normalizedScores = studentResults.stream()
                .map(result -> {
                    List<Integer> allScores = scoresByExamId.getOrDefault(result.getExam().getId(), List.of());

                    double mean = statisticsCalculator.calculateMean(allScores);
                    double stdDev = statisticsCalculator.calculateStandardDeviation(allScores);
                    double zScore = statisticsCalculator.calculateZScore(result.getTotalScore(), mean, stdDev);
                    double normalizedScore = statisticsCalculator.calculateNormalizedScore(zScore);

                    return new NormalizedScoreResponse.NormalizedExamScore(
                            result.getExam().getId(), result.getExam().getName(),
                            result.getCreatedAt(), result.getTotalScore(),
                            Math.round(mean * 10) / 10.0,
                            Math.round(stdDev * 10) / 10.0,
                            Math.round(zScore * 100) / 100.0,
                            Math.round(normalizedScore * 10) / 10.0
                    );
                })
                .sorted(Comparator.comparing(NormalizedScoreResponse.NormalizedExamScore::examDate))
                .toList();

        return new NormalizedScoreResponse(studentId, normalizedScores);
    }

    public GoalSimulationResponse simulateGoal(Long studentId, GoalSimulationRequest request) {
        log.debug("Simulating goal for student: {} with target grade: {}", studentId, request.targetGrade());

        List<ExamResult> studentResults = resultRepository.findByStudentId(studentId);
        if (studentResults.isEmpty()) {
            throw new ExamException(ExamErrorCode.RESULT_NOT_FOUND);
        }

        ExamResult latestResult = studentResults.stream()
                .max(Comparator.comparing(ExamResult::getCreatedAt))
                .orElseThrow(() -> new ExamException(ExamErrorCode.RESULT_NOT_FOUND));

        if (request.baseExamId() != null) {
            latestResult = resultRepository.findByExamIdAndStudentId(request.baseExamId(), studentId)
                    .orElse(latestResult);
        }

        Exam exam = latestResult.getExam();
        List<ExamResult> allResults = resultRepository.findByExamId(exam.getId());
        List<Integer> allScores = allResults.stream()
                .map(ExamResult::getTotalScore)
                .sorted(Comparator.reverseOrder())
                .toList();

        int currentScore = latestResult.getTotalScore();
        int currentGrade = latestResult.getGrade() != null ? latestResult.getGrade() : 5;
        int targetGrade = request.targetGrade();

        int targetScore = calculateTargetScoreForGrade(targetGrade, allScores);
        int scoreDifference = targetScore - currentScore;
        boolean achievable = scoreDifference <= calculateMaxImprovementPotential(exam, latestResult);

        List<GoalSimulationResponse.ImprovementScenario> scenarios = calculateImprovementScenarios(
                exam, latestResult, scoreDifference
        );

        return new GoalSimulationResponse(
                studentId, currentGrade, targetGrade, currentScore, targetScore,
                Math.max(0, scoreDifference), achievable, scenarios
        );
    }

    // === Private helpers ===

    private List<StudentRankResponse.TypePercentile> calculateTypePercentiles(
            Exam exam, ExamResult studentResult, List<ExamResult> allResults) {

        if (exam.getQuestionTypes() == null) {
            return Collections.emptyList();
        }

        Map<String, int[]> studentTypeStats = new HashMap<>();
        for (QuestionResult qr : studentResult.getQuestionResults()) {
            String type = exam.getQuestionType(qr.getQuestionNumber());
            if (type == null) continue;
            studentTypeStats.computeIfAbsent(type, k -> new int[]{0, 0});
            studentTypeStats.get(type)[1]++;
            if (qr.isCorrect()) {
                studentTypeStats.get(type)[0]++;
            }
        }

        Map<String, List<Double>> typeAccuracies = new HashMap<>();
        for (ExamResult result : allResults) {
            Map<String, int[]> resultTypeStats = new HashMap<>();
            for (QuestionResult qr : result.getQuestionResults()) {
                String type = exam.getQuestionType(qr.getQuestionNumber());
                if (type == null) continue;
                resultTypeStats.computeIfAbsent(type, k -> new int[]{0, 0});
                resultTypeStats.get(type)[1]++;
                if (qr.isCorrect()) {
                    resultTypeStats.get(type)[0]++;
                }
            }
            for (Map.Entry<String, int[]> entry : resultTypeStats.entrySet()) {
                double acc = entry.getValue()[1] > 0
                        ? (double) entry.getValue()[0] / entry.getValue()[1] * 100
                        : 0;
                typeAccuracies.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(acc);
            }
        }

        return studentTypeStats.entrySet().stream()
                .map(entry -> {
                    String type = entry.getKey();
                    int correct = entry.getValue()[0];
                    int total = entry.getValue()[1];
                    double accuracy = total > 0 ? (double) correct / total * 100 : 0;

                    List<Double> allAccuracies = typeAccuracies.getOrDefault(type, Collections.emptyList());
                    long lowerCount = allAccuracies.stream().filter(a -> a < accuracy).count();
                    double percentile = allAccuracies.isEmpty() ? 0 : (double) lowerCount / allAccuracies.size() * 100;

                    return new StudentRankResponse.TypePercentile(
                            type, correct, total,
                            Math.round(accuracy * 10) / 10.0,
                            Math.round(percentile * 10) / 10.0
                    );
                })
                .toList();
    }

    private int calculateTargetScoreForGrade(int targetGrade, List<Integer> sortedScores) {
        if (sortedScores.isEmpty()) return 0;

        int thresholdIndex = 0;
        for (int i = 0; i < GRADE_PERCENTILE_THRESHOLDS.length; i++) {
            if (i + 1 == targetGrade) {
                thresholdIndex = (int) (GRADE_PERCENTILE_THRESHOLDS[i] / 100.0 * sortedScores.size());
                break;
            }
        }

        return sortedScores.get(Math.min(thresholdIndex, sortedScores.size() - 1));
    }

    private int calculateMaxImprovementPotential(Exam exam, ExamResult result) {
        int totalPossible = exam.calculateTotalPossibleScore();
        return totalPossible - result.getTotalScore();
    }

    private List<GoalSimulationResponse.ImprovementScenario> calculateImprovementScenarios(
            Exam exam, ExamResult result, int scoreDifference) {

        if (exam.getQuestionTypes() == null || scoreDifference <= 0) {
            return Collections.emptyList();
        }

        Map<String, int[]> typeStats = new HashMap<>();
        Map<String, Integer> typeScores = new HashMap<>();

        for (QuestionResult qr : result.getQuestionResults()) {
            String type = exam.getQuestionType(qr.getQuestionNumber());
            if (type == null) continue;

            typeStats.computeIfAbsent(type, k -> new int[]{0, 0});
            typeStats.get(type)[1]++;
            if (qr.isCorrect()) {
                typeStats.get(type)[0]++;
            }

            int score = exam.getScoreForQuestion(qr.getQuestionNumber());
            typeScores.merge(type, score, Integer::sum);
        }

        return typeStats.entrySet().stream()
                .map(entry -> {
                    String type = entry.getKey();
                    int correct = entry.getValue()[0];
                    int total = entry.getValue()[1];
                    double currentAccuracy = total > 0 ? (double) correct / total * 100 : 0;

                    int incorrectCount = total - correct;
                    int avgScorePerQuestion = typeScores.getOrDefault(type, 0) / Math.max(total, 1);
                    int potentialGain = incorrectCount * avgScorePerQuestion;

                    int additionalNeeded = (int) Math.ceil((double) scoreDifference / avgScorePerQuestion);
                    additionalNeeded = Math.min(additionalNeeded, incorrectCount);

                    double targetAccuracy = total > 0 ? (double) (correct + additionalNeeded) / total * 100 : 0;

                    String priority;
                    if (incorrectCount > 0 && currentAccuracy < 50) {
                        priority = "HIGH";
                    } else if (incorrectCount > 0 && currentAccuracy < 70) {
                        priority = "MEDIUM";
                    } else {
                        priority = "LOW";
                    }

                    return new GoalSimulationResponse.ImprovementScenario(
                            type, correct, total,
                            Math.round(currentAccuracy * 10) / 10.0,
                            additionalNeeded, Math.round(targetAccuracy * 10) / 10.0,
                            potentialGain, priority
                    );
                })
                .sorted((a, b) -> {
                    int priorityOrder = getPriorityOrder(a.priority()) - getPriorityOrder(b.priority());
                    return priorityOrder != 0 ? priorityOrder : b.potentialScoreGain() - a.potentialScoreGain();
                })
                .toList();
    }

    private int getPriorityOrder(String priority) {
        return switch (priority) {
            case "HIGH" -> 1;
            case "MEDIUM" -> 2;
            case "LOW" -> 3;
            default -> 4;
        };
    }
}
