package com.lumie.exam.application.service;

import com.lumie.exam.application.dto.response.statistics.*;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.entity.QuestionResult;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.exception.ExamException;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.service.GradeCalculator;
import com.lumie.exam.domain.service.StatisticsCalculator;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ExamStatisticsQueryService {

    private final ExamRepository examRepository;
    private final ExamResultRepository resultRepository;
    private final StatisticsCalculator statisticsCalculator;
    private final GradeCalculator gradeCalculator;

    public ExamStatisticsResponse getExamStatistics(Long examId) {
        log.debug("Getting statistics for exam: {}", examId);

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamException(ExamErrorCode.EXAM_NOT_FOUND));

        List<ExamResult> results = resultRepository.findByExamId(examId);

        if (results.isEmpty()) {
            return new ExamStatisticsResponse(
                    examId, exam.getName(), exam.getCategory(), exam.getGradingType(), exam.getGradeScale(),
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
            gradeDistribution = calculateGradeDistribution(exam, results);
        }

        List<ExamStatisticsResponse.ScoreRangeDistribution> scoreRangeDistribution = calculateScoreRangeDistribution(scores);
        List<ExamStatisticsResponse.TypeAccuracy> typeAccuracyList = calculateExamTypeAccuracy(exam, results);
        List<ExamStatisticsResponse.TopIncorrectQuestion> topIncorrectQuestions = calculateExamTopIncorrect(exam, results);

        return new ExamStatisticsResponse(
                examId, exam.getName(), exam.getCategory(), exam.getGradingType(), exam.getGradeScale(),
                results.size(),
                Math.round(average * 10) / 10.0, highest, lowest,
                Math.round(stdDev * 10) / 10.0,
                passRate, passCount, failCount,
                gradeDistribution, scoreRangeDistribution, typeAccuracyList, topIncorrectQuestions
        );
    }

    public ChoiceDistributionResponse getChoiceDistribution(Long examId) {
        log.debug("Getting choice distribution for exam: {}", examId);

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamException(ExamErrorCode.EXAM_NOT_FOUND));

        List<ExamResult> results = resultRepository.findByExamId(examId);
        int totalParticipants = results.size();

        Map<Integer, Map<String, Integer>> questionChoiceCounts = new HashMap<>();

        for (ExamResult result : results) {
            for (QuestionResult qr : result.getQuestionResults()) {
                int qNum = qr.getQuestionNumber();
                String choice = qr.getSelectedChoice();
                if (choice == null) choice = "NO_ANSWER";

                questionChoiceCounts.computeIfAbsent(qNum, k -> new HashMap<>());
                questionChoiceCounts.get(qNum).merge(choice, 1, Integer::sum);
            }
        }

        List<ChoiceDistributionResponse.QuestionChoiceDistribution> questions = new ArrayList<>();

        for (int qNum = 1; qNum <= exam.getTotalQuestions(); qNum++) {
            String correctAnswer = exam.getCorrectAnswerForQuestion(qNum);
            String questionType = exam.getQuestionType(qNum);
            Map<String, Integer> choiceCounts = questionChoiceCounts.getOrDefault(qNum, new HashMap<>());

            int correctCount = choiceCounts.getOrDefault(correctAnswer, 0);
            double correctRate = statisticsCalculator.calculateChoiceRate(correctCount, totalParticipants);

            Map<String, ChoiceDistributionResponse.ChoiceStats> choiceDistribution = new LinkedHashMap<>();
            for (String choice : List.of("1", "2", "3", "4", "5")) {
                int count = choiceCounts.getOrDefault(choice, 0);
                double percentage = statisticsCalculator.calculateChoiceRate(count, totalParticipants);
                choiceDistribution.put(choice, new ChoiceDistributionResponse.ChoiceStats(
                        count, Math.round(percentage * 10) / 10.0, choice.equals(correctAnswer)
                ));
            }

            List<ChoiceDistributionResponse.AttractiveDistractor> attractiveDistractors = choiceCounts.entrySet().stream()
                    .filter(e -> !e.getKey().equals(correctAnswer) && !e.getKey().equals("NO_ANSWER"))
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .limit(2)
                    .map(e -> new ChoiceDistributionResponse.AttractiveDistractor(
                            e.getKey(), e.getValue(),
                            Math.round(statisticsCalculator.calculateChoiceRate(e.getValue(), totalParticipants) * 10) / 10.0
                    ))
                    .toList();

            questions.add(new ChoiceDistributionResponse.QuestionChoiceDistribution(
                    qNum, questionType, correctAnswer, correctCount,
                    Math.round(correctRate * 10) / 10.0, choiceDistribution, attractiveDistractors
            ));
        }

        return new ChoiceDistributionResponse(examId, exam.getName(), totalParticipants, questions);
    }

    public DashboardStatisticsResponse getDashboardStatistics() {
        log.debug("Getting dashboard statistics");

        List<Exam> allExams = examRepository.findAll();
        List<ExamResult> allResults = new ArrayList<>();
        for (Exam exam : allExams) {
            allResults.addAll(resultRepository.findByExamId(exam.getId()));
        }

        if (allResults.isEmpty()) {
            // TODO: Exam entity has no active/published status field; activeExams equals totalExams until a status concept is added
            return new DashboardStatisticsResponse(
                    allExams.size(), 0, 0.0,
                    Collections.emptyList(), Collections.emptyList(),
                    allExams.size(),
                    new DashboardStatisticsResponse.GradeDistributionByScale(
                            buildGradeBuckets(Collections.emptyMap(), 5),
                            buildGradeBuckets(Collections.emptyMap(), 9)
                    )
            );
        }

        double overallAverage = allResults.stream()
                .mapToInt(ExamResult::getTotalScore)
                .average()
                .orElse(0.0);

        Map<String, int[]> typeStats = new HashMap<>();
        Map<String, QuestionErrorInfo> questionErrors = new HashMap<>();

        for (ExamResult result : allResults) {
            Exam exam = result.getExam();
            for (QuestionResult qr : result.getQuestionResults()) {
                String type = exam.getQuestionType(qr.getQuestionNumber());
                if (type != null) {
                    typeStats.computeIfAbsent(type, k -> new int[]{0, 0});
                    typeStats.get(type)[1]++;
                    if (qr.isCorrect()) {
                        typeStats.get(type)[0]++;
                    }
                }

                String questionKey = exam.getId() + "-" + qr.getQuestionNumber();
                QuestionErrorInfo errorInfo = questionErrors.computeIfAbsent(questionKey,
                        k -> new QuestionErrorInfo(exam, qr.getQuestionNumber()));
                errorInfo.totalAttempts++;
                if (!qr.isCorrect()) {
                    errorInfo.incorrectCount++;
                    if (qr.getSelectedChoice() != null) {
                        errorInfo.wrongChoices.merge(qr.getSelectedChoice(), 1, Integer::sum);
                    }
                }
            }
        }

        List<DashboardStatisticsResponse.TypeAccuracy> typeAccuracyList = typeStats.entrySet().stream()
                .map(entry -> {
                    int correct = entry.getValue()[0];
                    int total = entry.getValue()[1];
                    double accuracy = total > 0 ? (double) correct / total * 100 : 0;
                    return new DashboardStatisticsResponse.TypeAccuracy(
                            entry.getKey(), total, correct, Math.round(accuracy * 10) / 10.0
                    );
                })
                .sorted((a, b) -> Double.compare(b.accuracyRate(), a.accuracyRate()))
                .toList();

        List<DashboardStatisticsResponse.TopIncorrectQuestion> topIncorrect = questionErrors.values().stream()
                .filter(info -> info.totalAttempts >= 5)
                .sorted((a, b) -> {
                    double rateA = (double) a.incorrectCount / a.totalAttempts;
                    double rateB = (double) b.incorrectCount / b.totalAttempts;
                    return Double.compare(rateB, rateA);
                })
                .limit(10)
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
                            info.totalAttempts, info.incorrectCount,
                            Math.round(incorrectRate * 10) / 10.0,
                            topWrongChoice, Math.round(topWrongRate * 10) / 10.0
                    );
                })
                .toList();

        Map<Integer, Integer> fiveBuckets = new HashMap<>();
        Map<Integer, Integer> nineBuckets = new HashMap<>();
        for (ExamResult result : allResults) {
            Exam exam = result.getExam();
            Integer grade = result.getGrade();
            GradeScale scale = exam.getGradeScale();
            if (grade == null || grade <= 0 || scale == null) continue;
            if (scale == GradeScale.FIVE_GRADE) {
                fiveBuckets.merge(grade, 1, Integer::sum);
            } else if (scale == GradeScale.NINE_GRADE) {
                nineBuckets.merge(grade, 1, Integer::sum);
            }
        }

        DashboardStatisticsResponse.GradeDistributionByScale gradeDistribution =
                new DashboardStatisticsResponse.GradeDistributionByScale(
                        buildGradeBuckets(fiveBuckets, 5),
                        buildGradeBuckets(nineBuckets, 9)
                );

        // TODO: Exam entity has no active/published status field; activeExams equals totalExams until a status concept is added
        return new DashboardStatisticsResponse(
                allExams.size(), allResults.size(),
                Math.round(overallAverage * 10) / 10.0,
                typeAccuracyList, topIncorrect,
                allExams.size(),
                gradeDistribution
        );
    }

    private List<DashboardStatisticsResponse.GradeBucket> buildGradeBuckets(
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

    // === Private helpers ===

    private static class QuestionErrorInfo {
        final Exam exam;
        final int questionNumber;
        int totalAttempts = 0;
        int incorrectCount = 0;
        Map<String, Integer> wrongChoices = new HashMap<>();

        QuestionErrorInfo(Exam exam, int questionNumber) {
            this.exam = exam;
            this.questionNumber = questionNumber;
        }
    }

    private static class PassFailStats {
        final double passRate;
        final int passCount;
        final int failCount;

        PassFailStats(double passRate, int passCount, int failCount) {
            this.passRate = passRate;
            this.passCount = passCount;
            this.failCount = failCount;
        }
    }

    private PassFailStats calculatePassFailStats(Exam exam, List<ExamResult> results) {
        if (exam.getCategory() != ExamCategory.PASS_FAIL || results.isEmpty()) {
            return new PassFailStats(0.0, 0, 0);
        }

        int passCount = (int) results.stream()
                .filter(ExamResult::isPassed)
                .count();
        int failCount = results.size() - passCount;
        double passRate = (double) passCount / results.size() * 100;

        return new PassFailStats(Math.round(passRate * 10) / 10.0, passCount, failCount);
    }

    private List<ExamStatisticsResponse.GradeDistribution> calculateGradeDistribution(Exam exam, List<ExamResult> results) {
        List<Integer> allScores = results.stream()
                .map(ExamResult::getTotalScore)
                .toList();

        GradingType gradingType = exam.getGradingType() != null ? exam.getGradingType() : GradingType.ABSOLUTE;
        GradeScale gradeScale = exam.getGradeScale() != null ? exam.getGradeScale() : GradeScale.NINE_GRADE;

        Map<Integer, Long> gradeCounts = results.stream()
                .map(r -> {
                    if (exam.getCategory() == ExamCategory.GRADED &&
                        exam.getGradingType() == GradingType.RELATIVE) {
                        return gradeCalculator.calculateRelativeGrade(r.getTotalScore(), allScores, gradeScale);
                    }
                    return r.getGrade();
                })
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
