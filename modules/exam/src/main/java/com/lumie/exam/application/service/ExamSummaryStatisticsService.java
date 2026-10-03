package com.lumie.exam.application.service;

import com.lumie.common.cache.CacheConfig;
import com.lumie.exam.application.dto.response.statistics.DashboardStatisticsResponse;
import com.lumie.exam.application.dto.response.statistics.ExamStatisticsResponse;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.entity.QuestionResult;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.common.exception.BusinessException;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.repository.ExamResultRepository.GradeBucketRow;
import com.lumie.exam.domain.repository.ExamResultRepository.QuestionChoiceCountRow;
import com.lumie.exam.domain.repository.ExamResultRepository.ResultAggregateRow;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.ExamSource;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Top-level summary statistics for one exam ({@code getExamStatistics}) and
 * the cross-exam dashboard view ({@code getDashboardStatistics}).
 *
 * <p>Responsible for: averages / std dev / score range distributions,
 * pass-fail counts, grade buckets, type accuracy, top-incorrect questions —
 * across one exam (single) or all exams (dashboard).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ExamSummaryStatisticsService {

    private static final int MIN_ATTEMPTS_FOR_TOP_INCORRECT = 5;
    private static final int TOP_INCORRECT_LIMIT = 10;

    private final ExamRepository examRepository;
    private final ExamResultRepository resultRepository;
    private final StatisticsCalculator statisticsCalculator;
    private final GradeCalculator gradeCalculator;
    private final ExamResultCohortResolver cohortResolver;

    public ExamStatisticsResponse getExamStatistics(Long examId) {
        return getExamStatistics(examId, null, null, null);
    }

    public ExamStatisticsResponse getExamStatistics(
            Long examId,
            Long classId,
            Boolean hasActiveEnrollment,
            Boolean isActive) {
        log.debug("Getting statistics for exam: {}", examId);

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.EXAM_NOT_FOUND));

        List<ExamResult> allResults = resultRepository.findByExamId(examId);
        List<ExamResult> results = filterResults(allResults, classId, hasActiveEnrollment, isActive);

        return buildExamStatistics(exam, results, allResults);
    }

    ExamStatisticsResponse buildExamStatistics(Exam exam, List<ExamResult> results) {
        return buildExamStatistics(exam, results, results);
    }

    private List<ExamResult> filterResults(
            List<ExamResult> results,
            Long classId,
            Boolean hasActiveEnrollment,
            Boolean isActive) {
        if (classId == null && hasActiveEnrollment == null && isActive == null) {
            return results;
        }
        return cohortResolver.filter(results, classId, hasActiveEnrollment, isActive);
    }

    private ExamStatisticsResponse buildExamStatistics(
            Exam exam,
            List<ExamResult> results,
            List<ExamResult> allExamResults) {
        if (results.isEmpty()) {
            return new ExamStatisticsResponse(
                    exam.getId(), exam.getName(), exam.getCategory(), exam.getGradingType(), exam.getGradeScale(),
                    0, 0.0, 0, 0, 0.0,
                    null, null, null,
                    Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyList()
            );
        }

        List<Integer> scores = results.stream()
                .map(ExamResult::getTotalScore)
                .toList();

        double average = statisticsCalculator.calculateMean(scores);
        int highest = scores.stream().max(Integer::compareTo).orElse(0);
        int lowest = scores.stream().min(Integer::compareTo).orElse(0);
        double stdDev = statisticsCalculator.calculateStandardDeviation(scores);

        Double passRate = null;
        Integer passCount = null;
        Integer failCount = null;
        List<ExamStatisticsResponse.GradeDistribution> gradeDistribution = Collections.emptyList();

        if (exam.getCategory() == ExamCategory.PASS_FAIL) {
            PassFailStats pfStats = calculatePassFailStats(exam, results);
            passRate = pfStats.passRate;
            passCount = pfStats.passCount;
            failCount = pfStats.failCount;
        } else {
            gradeDistribution = calculateGradeDistribution(exam, results, allExamResults);
        }

        List<ExamStatisticsResponse.ScoreRangeDistribution> scoreRangeDistribution = calculateScoreRangeDistribution(scores);
        List<ExamStatisticsResponse.TypeAccuracy> typeAccuracyList = calculateExamTypeAccuracy(exam, results);
        List<ExamStatisticsResponse.TopIncorrectQuestion> topIncorrectQuestions = calculateExamTopIncorrect(exam, results);

        return new ExamStatisticsResponse(
                exam.getId(), exam.getName(), exam.getCategory(), exam.getGradingType(), exam.getGradeScale(),
                results.size(),
                Math.round(average * 10) / 10.0, highest, lowest,
                Math.round(stdDev * 10) / 10.0,
                passRate, passCount, failCount,
                gradeDistribution, scoreRangeDistribution, typeAccuracyList, topIncorrectQuestions
        );
    }

    /**
     * Cross-exam dashboard statistics, computed directly with tenant-scoped SQL
     * aggregates (COUNT / SUM / GROUP BY) and assembled in memory. Heap stays
     * bounded by the number of exams (loaded for their question metadata) plus
     * the compact aggregate rows — never by the number of results or
     * question-results, which is what caused the original OOM. Because the
     * query is RLS-scoped, each call only ever aggregates one tenant's rows.
     *
     * <p>Cached per tenant with a short TTL ({@link CacheConfig#CACHE_EXAM_DASHBOARD}):
     * dashboard figures tolerate a few minutes of staleness, so no write-path
     * eviction is needed.
     */
    @Cacheable(value = CacheConfig.CACHE_EXAM_DASHBOARD,
            key = "T(com.lumie.common.tenant.TenantContextHolder).getRequiredTenantId()")
    public DashboardStatisticsResponse getDashboardStatistics() {
        log.debug("Computing dashboard statistics from SQL aggregates");

        List<Exam> exams = examRepository.findBySource(ExamSource.EXAM_MANAGEMENT);
        int totalExams = exams.size();

        ResultAggregateRow agg = resultRepository.aggregateResults();
        long totalResults = agg != null ? agg.getResultCount() : 0L;
        long scoreSum = agg != null ? agg.getScoreSum() : 0L;
        if (totalResults == 0) {
            return emptyDashboard(totalExams);
        }
        double overallAverage = (double) scoreSum / totalResults;

        Map<Long, Exam> examById = exams.stream()
                .collect(Collectors.toMap(Exam::getId, e -> e));

        // Grade buckets, routed by each exam's scale.
        Map<Integer, Integer> fiveBuckets = new HashMap<>();
        Map<Integer, Integer> nineBuckets = new HashMap<>();
        for (GradeBucketRow row : resultRepository.aggregateGradeBuckets()) {
            if (row.getGrade() == null || row.getCnt() == null) continue;
            if (GradeScale.FIVE_GRADE.name().equals(row.getGradeScale())) {
                fiveBuckets.merge(row.getGrade(), row.getCnt(), Integer::sum);
            } else if (GradeScale.NINE_GRADE.name().equals(row.getGradeScale())) {
                nineBuckets.merge(row.getGrade(), row.getCnt(), Integer::sum);
            }
        }

        // Type accuracy + per-question incorrect distribution, from the compact
        // (examId, questionNumber, selectedChoice, isCorrect) counts.
        Map<String, int[]> typeStats = new HashMap<>();              // type -> [correct, total] (same as calculateExamTypeAccuracy)
        Map<String, QuestionErrorInfo> questionErrors = new HashMap<>();
        for (QuestionChoiceCountRow row : resultRepository.aggregateQuestionChoices()) {
            Exam exam = examById.get(row.getExamId());
            if (exam == null) continue;
            int qNum = row.getQuestionNumber();
            int cnt = row.getCnt() != null ? row.getCnt() : 0;
            boolean correct = Boolean.TRUE.equals(row.getCorrect());

            String type = exam.getQuestionType(qNum);
            if (type != null) {
                int[] t = typeStats.computeIfAbsent(type, k -> new int[]{0, 0});
                t[1] += cnt;                 // total
                if (correct) t[0] += cnt;    // correct
            }

            QuestionErrorInfo info = questionErrors.computeIfAbsent(
                    exam.getId() + "-" + qNum, k -> new QuestionErrorInfo(exam, qNum));
            info.totalAttempts += cnt;
            if (!correct) {
                info.incorrectCount += cnt;
                String choice = row.getSelectedChoice();
                if (choice != null) {
                    info.wrongChoices.merge(choice, cnt, Integer::sum);
                }
            }
        }

        List<DashboardStatisticsResponse.TypeAccuracy> typeAccuracyList = typeStats.entrySet().stream()
                .map(entry -> {
                    int correct = entry.getValue()[0];
                    int total = entry.getValue()[1];
                    double accuracy = total > 0 ? (double) correct / total * 100 : 0;
                    return new DashboardStatisticsResponse.TypeAccuracy(
                            entry.getKey(), total, correct, round1(accuracy));
                })
                .sorted((a, b) -> Double.compare(b.accuracyRate(), a.accuracyRate()))
                .toList();

        List<DashboardStatisticsResponse.TopIncorrectQuestion> topIncorrect = questionErrors.values().stream()
                .filter(info -> info.totalAttempts >= MIN_ATTEMPTS_FOR_TOP_INCORRECT)
                .sorted((a, b) -> Double.compare(
                        (double) b.incorrectCount / b.totalAttempts,
                        (double) a.incorrectCount / a.totalAttempts))
                .limit(TOP_INCORRECT_LIMIT)
                .map(info -> {
                    double incorrectRate = (double) info.incorrectCount / info.totalAttempts * 100;
                    String topWrongChoice = info.wrongChoices.entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse("-");
                    int topWrongCount = info.wrongChoices.getOrDefault(topWrongChoice, 0);
                    double topWrongRate = info.incorrectCount > 0
                            ? (double) topWrongCount / info.incorrectCount * 100 : 0;
                    return new DashboardStatisticsResponse.TopIncorrectQuestion(
                            info.exam.getId(), info.exam.getName(), info.questionNumber,
                            info.exam.getQuestionType(info.questionNumber),
                            info.exam.getCorrectAnswerForQuestion(info.questionNumber),
                            info.totalAttempts, info.incorrectCount, round1(incorrectRate),
                            topWrongChoice, round1(topWrongRate));
                })
                .toList();

        DashboardStatisticsResponse.GradeDistributionByScale gradeDistribution =
                new DashboardStatisticsResponse.GradeDistributionByScale(
                        buildDashboardGradeBuckets(fiveBuckets, 5),
                        buildDashboardGradeBuckets(nineBuckets, 9));

        return new DashboardStatisticsResponse(
                totalExams, (int) totalResults, round1(overallAverage),
                typeAccuracyList, topIncorrect, gradeDistribution);
    }

    private DashboardStatisticsResponse emptyDashboard(int totalExams) {
        return new DashboardStatisticsResponse(
                totalExams, 0, 0.0,
                Collections.emptyList(), Collections.emptyList(),
                new DashboardStatisticsResponse.GradeDistributionByScale(
                        buildDashboardGradeBuckets(Collections.emptyMap(), 5),
                        buildDashboardGradeBuckets(Collections.emptyMap(), 9)));
    }

    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private static class QuestionErrorInfo {
        final Exam exam;
        final int questionNumber;
        int totalAttempts = 0;
        int incorrectCount = 0;
        final Map<String, Integer> wrongChoices = new HashMap<>();

        QuestionErrorInfo(Exam exam, int questionNumber) {
            this.exam = exam;
            this.questionNumber = questionNumber;
        }
    }

    private List<DashboardStatisticsResponse.GradeBucket> buildDashboardGradeBuckets(
            Map<Integer, Integer> counts, int maxGrade) {
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        List<DashboardStatisticsResponse.GradeBucket> buckets = new ArrayList<>(maxGrade);
        for (int grade = 1; grade <= maxGrade; grade++) {
            int count = counts.getOrDefault(grade, 0);
            double pct = total > 0 ? Math.round((double) count / total * 1000) / 10.0 : 0.0;
            buckets.add(new DashboardStatisticsResponse.GradeBucket(grade, count, pct));
        }
        return buckets;
    }

    private record PassFailStats(Double passRate, Integer passCount, Integer failCount) {}

    private PassFailStats calculatePassFailStats(Exam exam, List<ExamResult> results) {
        if (exam.getCategory() != ExamCategory.PASS_FAIL || results.isEmpty()) {
            return new PassFailStats(null, null, null);
        }
        if (exam.getPassScore() == null) {
            // Historical missing cutoffs require reconciliation; they are not failures.
            return new PassFailStats(null, null, null);
        }
        int passCount = (int) results.stream().filter(ExamResult::isPassed).count();
        int failCount = results.size() - passCount;
        double passRate = (double) passCount / results.size() * 100;
        return new PassFailStats(Math.round(passRate * 10) / 10.0, passCount, failCount);
    }

    private List<ExamStatisticsResponse.GradeDistribution> calculateGradeDistribution(
            Exam exam,
            List<ExamResult> results,
            List<ExamResult> allExamResults) {
        List<Integer> allScores = allExamResults.stream().map(ExamResult::getTotalScore).toList();

        GradingType gradingType = exam.getGradingType() != null ? exam.getGradingType() : GradingType.ABSOLUTE;
        GradeScale gradeScale = exam.getGradeScale() != null ? exam.getGradeScale() : GradeScale.NINE_GRADE;

        Map<Integer, Long> gradeCounts = results.stream()
                .map(r -> gradeCalculator.resolveStoredOrRelativeGrade(exam, r, allScores))
                .filter(grade -> grade != null && grade > 0)
                .collect(Collectors.groupingBy(grade -> grade, Collectors.counting()));

        return gradeCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new ExamStatisticsResponse.GradeDistribution(
                        entry.getKey(), entry.getValue().intValue(),
                        Math.round((double) entry.getValue() / results.size() * 1000) / 10.0,
                        gradeCalculator.calculateCutoffScore(entry.getKey(), allScores, gradingType, gradeScale)
                ))
                .toList();
    }

    private List<ExamStatisticsResponse.ScoreRangeDistribution> calculateScoreRangeDistribution(List<Integer> scores) {
        int total = scores.size();
        Map<String, Integer> rangeCounts = new LinkedHashMap<>();
        rangeCounts.put("90-100", 0);
        rangeCounts.put("80-89", 0);
        rangeCounts.put("70-79", 0);
        rangeCounts.put("60-69", 0);
        rangeCounts.put("0-59", 0);

        for (int score : scores) {
            if (score >= 90) rangeCounts.merge("90-100", 1, Integer::sum);
            else if (score >= 80) rangeCounts.merge("80-89", 1, Integer::sum);
            else if (score >= 70) rangeCounts.merge("70-79", 1, Integer::sum);
            else if (score >= 60) rangeCounts.merge("60-69", 1, Integer::sum);
            else rangeCounts.merge("0-59", 1, Integer::sum);
        }

        return rangeCounts.entrySet().stream()
                .map(entry -> new ExamStatisticsResponse.ScoreRangeDistribution(
                        entry.getKey(), entry.getValue(),
                        Math.round((double) entry.getValue() / total * 1000) / 10.0
                ))
                .toList();
    }

    private List<ExamStatisticsResponse.TypeAccuracy> calculateExamTypeAccuracy(Exam exam, List<ExamResult> results) {
        if (exam.getQuestionTypes() == null) {
            return Collections.emptyList();
        }

        Map<String, int[]> typeStats = new HashMap<>();

        for (ExamResult result : results) {
            for (QuestionResult qr : result.getQuestionResults()) {
                String type = exam.getQuestionType(qr.getQuestionNumber());
                if (type != null) {
                    typeStats.computeIfAbsent(type, k -> new int[]{0, 0});
                    typeStats.get(type)[1]++;
                    if (qr.isCorrect()) {
                        typeStats.get(type)[0]++;
                    }
                }
            }
        }

        return typeStats.entrySet().stream()
                .map(entry -> {
                    int correct = entry.getValue()[0];
                    int total = entry.getValue()[1];
                    double accuracy = total > 0 ? (double) correct / total * 100 : 0;
                    return new ExamStatisticsResponse.TypeAccuracy(
                            entry.getKey(), total, correct, Math.round(accuracy * 10) / 10.0
                    );
                })
                .sorted((a, b) -> Double.compare(b.accuracyRate(), a.accuracyRate()))
                .toList();
    }

    private List<ExamStatisticsResponse.TopIncorrectQuestion> calculateExamTopIncorrect(Exam exam, List<ExamResult> results) {
        int totalParticipants = results.size();
        if (totalParticipants == 0) {
            return Collections.emptyList();
        }

        Map<Integer, Map<String, Integer>> questionChoiceCounts = new HashMap<>();
        Map<Integer, Integer> questionCorrectCounts = new HashMap<>();

        for (ExamResult result : results) {
            for (QuestionResult qr : result.getQuestionResults()) {
                int qNum = qr.getQuestionNumber();
                String choice = qr.getSelectedChoice();
                if (choice == null) choice = "NO_ANSWER";

                questionChoiceCounts.computeIfAbsent(qNum, k -> new HashMap<>());
                questionChoiceCounts.get(qNum).merge(choice, 1, Integer::sum);

                if (qr.isCorrect()) {
                    questionCorrectCounts.merge(qNum, 1, Integer::sum);
                }
            }
        }

        List<ExamStatisticsResponse.TopIncorrectQuestion> topIncorrect = new ArrayList<>();

        for (int qNum = 1; qNum <= exam.getTotalQuestions(); qNum++) {
            String correctAnswer = exam.getCorrectAnswerForQuestion(qNum);
            String questionType = exam.getQuestionType(qNum);
            Map<String, Integer> choiceCounts = questionChoiceCounts.getOrDefault(qNum, new HashMap<>());

            int correctCount = questionCorrectCounts.getOrDefault(qNum, 0);
            int incorrectCount = totalParticipants - correctCount;
            double incorrectRate = (double) incorrectCount / totalParticipants * 100;

            String topWrongChoice = "-";
            int topWrongCount = 0;
            for (Map.Entry<String, Integer> entry : choiceCounts.entrySet()) {
                if (!entry.getKey().equals(correctAnswer) && !entry.getKey().equals("NO_ANSWER")) {
                    if (entry.getValue() > topWrongCount) {
                        topWrongCount = entry.getValue();
                        topWrongChoice = entry.getKey();
                    }
                }
            }
            double topWrongRate = incorrectCount > 0 ? (double) topWrongCount / incorrectCount * 100 : 0;

            topIncorrect.add(new ExamStatisticsResponse.TopIncorrectQuestion(
                    qNum, questionType, correctAnswer, totalParticipants, incorrectCount,
                    Math.round(incorrectRate * 10) / 10.0,
                    topWrongChoice, Math.round(topWrongRate * 10) / 10.0
            ));
        }

        return topIncorrect.stream()
                .filter(q -> q.incorrectCount() > 0)
                .sorted((a, b) -> Double.compare(b.incorrectRate(), a.incorrectRate()))
                .limit(10)
                .toList();
    }
}
