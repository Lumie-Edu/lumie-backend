package com.lumie.exam.application.service;

import com.lumie.exam.application.dto.response.statistics.*;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.entity.QuestionResult;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.common.exception.BusinessException;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.vo.ExamCategory;
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

    public StudentRankResponse getStudentRank(Long studentId, Long examId) {
        log.debug("Getting rank for student: {} in exam: {}", studentId, examId);

        ExamResult result = resultRepository.findByExamIdAndStudentId(examId, studentId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.RESULT_NOT_FOUND));

        Exam exam = result.getExam();
        List<ExamResult> allResults = resultRepository.findByExamId(examId);
        List<Integer> allScores = allResults.stream()
                .map(ExamResult::getTotalScore)
                .toList();

        int rank = statisticsCalculator.calculateRank(result.getTotalScore(), allScores);
        double percentile = statisticsCalculator.calculatePercentile(result.getTotalScore(), allScores);

        Integer grade = gradeCalculator.resolveStoredOrRelativeGrade(exam, result, allScores);

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
        Set<Long> relativeExamIds = results.stream()
                .filter(result -> result.getExam().getCategory() == ExamCategory.GRADED
                        && result.getExam().getGradingType() == GradingType.RELATIVE)
                .map(result -> result.getExam().getId())
                .collect(java.util.stream.Collectors.toSet());
        Map<Long, List<Integer>> relativeScoresByExamId = relativeExamIds.isEmpty()
                ? Map.of()
                : resultRepository.findByExamIdIn(relativeExamIds).stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                result -> result.getExam().getId(),
                                java.util.stream.Collectors.mapping(ExamResult::getTotalScore,
                                        java.util.stream.Collectors.toList())));

        return ExamStatisticsProjection.calculateStabilityIndex(
                studentId, results, statisticsCalculator, gradeCalculator, relativeScoresByExamId);
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

    private List<StudentRankResponse.TypePercentile> calculateTypePercentiles(
            Exam exam, ExamResult studentResult, List<ExamResult> allResults) {
        return ExamStatisticsProjection.calculateTypePercentiles(exam, studentResult,
                ExamStatisticsProjection.calculateTypeAccuracies(exam, allResults));
    }

}
