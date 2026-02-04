package com.lumie.exam.application.service;

import com.lumie.exam.adapter.out.persistence.AcademyComparisonProjection;
import com.lumie.exam.application.dto.request.GoalSimulationRequest;
import com.lumie.exam.application.dto.response.statistics.*;
import com.lumie.exam.application.port.out.StudentServicePort;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StatisticsQueryService {

    private final ExamRepository examRepository;
    private final ExamResultRepository resultRepository;
    private final StatisticsCalculator statisticsCalculator;
    private final GradeCalculator gradeCalculator;
    private final StudentServicePort studentServicePort;

    private static final int[] GRADE_PERCENTILE_THRESHOLDS = {4, 11, 23, 40, 60, 77, 89, 96, 100};

    /**
     * 시험 전체 통계 조회
     */
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

        // P/NP 시험 전용 통계
        Double passRate = null;
        Integer passCount = null;
        Integer failCount = null;

        // 등급제 시험 전용 통계
        List<ExamStatisticsResponse.GradeDistribution> gradeDistribution = Collections.emptyList();

        if (exam.getCategory() == ExamCategory.PASS_FAIL) {
            // P/NP 시험: 합격/불합격 통계만 계산
            PassFailStats pfStats = calculatePassFailStats(exam, results);
            passRate = pfStats.passRate;
            passCount = pfStats.passCount;
            failCount = pfStats.failCount;
        } else {
            // 등급제 시험: 등급 분포 계산
            gradeDistribution = calculateGradeDistribution(exam, results);
        }

        List<ExamStatisticsResponse.ScoreRangeDistribution> scoreRangeDistribution = calculateScoreRangeDistribution(scores);

        // 유형별 정답률 계산
        List<ExamStatisticsResponse.TypeAccuracy> typeAccuracyList = calculateExamTypeAccuracy(exam, results);

        // 오답률 TOP 문항 계산
        List<ExamStatisticsResponse.TopIncorrectQuestion> topIncorrectQuestions = calculateExamTopIncorrect(exam, results);

        return new ExamStatisticsResponse(
                examId,
                exam.getName(),
                exam.getCategory(),
                exam.getGradingType(),
                exam.getGradeScale(),
                results.size(),
                Math.round(average * 10) / 10.0,
                highest,
                lowest,
                Math.round(stdDev * 10) / 10.0,
                passRate,
                passCount,
                failCount,
                gradeDistribution,
                scoreRangeDistribution,
                typeAccuracyList,
                topIncorrectQuestions
        );
    }

    /**
     * 시험별 학생 성적 목록 (페이지네이션)
     */
    public Page<StudentGradeResponse> getStudentGrades(Long examId, Pageable pageable) {
        log.debug("Getting student grades for exam: {}", examId);

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamException(ExamErrorCode.EXAM_NOT_FOUND));

        List<ExamResult> results = resultRepository.findByExamId(examId);
        List<Integer> allScores = results.stream()
                .map(ExamResult::getTotalScore)
                .toList();

        GradeScale gradeScale = exam.getGradeScale() != null ? exam.getGradeScale() : GradeScale.NINE_GRADE;

        List<StudentGradeResponse> grades = results.stream()
                .map(result -> {
                    StudentServicePort.StudentInfo student = getStudentInfoSafely(result.getStudentId());
                    int rank = statisticsCalculator.calculateRank(result.getTotalScore(), allScores);
                    double percentile = statisticsCalculator.calculatePercentile(result.getTotalScore(), allScores);

                    // 상대평가는 조회 시 계산, 절대평가/합불제는 DB값 사용
                    Integer grade = result.getGrade();
                    if (exam.getCategory() == ExamCategory.GRADED &&
                        exam.getGradingType() == GradingType.RELATIVE) {
                        grade = gradeCalculator.calculateRelativeGrade(result.getTotalScore(), allScores, gradeScale);
                    }

                    return new StudentGradeResponse(
                            result.getStudentId(),
                            student.name(),
                            student.phone(),
                            result.getTotalScore(),
                            rank,
                            Math.round(percentile * 10) / 10.0,
                            grade,
                            exam.getCategory(),
                            result.isPassed(),
                            result.getCreatedAt()
                    );
                })
                .sorted(exam.getCategory() == ExamCategory.PASS_FAIL
                        // P/NP 시험: 불합격자 먼저 (isPassed=false first), 그 다음 점수 오름차순
                        ? Comparator.comparing(StudentGradeResponse::isPassed)
                                .thenComparingInt(StudentGradeResponse::score)
                        // 등급제 시험: 순위 기준 정렬
                        : Comparator.comparingInt(StudentGradeResponse::rank))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), grades.size());
        List<StudentGradeResponse> pageContent = grades.subList(start, end);

        return new PageImpl<>(pageContent, pageable, grades.size());
    }

    private StudentServicePort.StudentInfo getStudentInfoSafely(Long studentId) {
        try {
            StudentServicePort.StudentInfo info = studentServicePort.getStudentInfo(studentId);
            if (info != null) {
                return info;
            }
        } catch (Exception e) {
            log.warn("Failed to get student info for studentId: {}", studentId, e);
        }
        return new StudentServicePort.StudentInfo(studentId, "학생 " + studentId, null, null);
    }

    /**
     * 학생 백분위/석차 조회
     */
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

        List<StudentRankResponse.TypePercentile> typePercentiles = calculateTypePercentiles(exam, result, allResults);

        return new StudentRankResponse(
                studentId,
                examId,
                exam.getName(),
                result.getTotalScore(),
                rank,
                allResults.size(),
                Math.round(percentile * 10) / 10.0,
                typePercentiles
        );
    }

    /**
     * 성적 안정성 지표 조회
     */
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
                        r.getExam().getId(),
                        r.getExam().getName(),
                        r.getTotalScore(),
                        r.getGrade() != null ? r.getGrade() : 0,
                        r.getCreatedAt()
                ))
                .toList();

        return new StabilityIndexResponse(
                studentId,
                results.size(),
                Math.round(average * 10) / 10.0,
                Math.round(stdDev * 10) / 10.0,
                Math.round(cv * 10) / 10.0,
                stabilityLevel,
                history
        );
    }

    /**
     * 유형별 성장 추이 조회
     */
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
                        exam.getId(),
                        exam.getName(),
                        result.getCreatedAt(),
                        correct,
                        total,
                        Math.round(accuracy * 10) / 10.0
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
                            entry.getKey(),
                            points,
                            Math.round(growthRate * 10) / 10.0
                    );
                })
                .toList();

        return new TypeGrowthTrendResponse(studentId, trends);
    }

    /**
     * Z-score 정규화 점수 조회
     */
    public NormalizedScoreResponse getNormalizedScores(Long studentId) {
        log.debug("Getting normalized scores for student: {}", studentId);

        List<ExamResult> studentResults = resultRepository.findByStudentId(studentId);

        List<NormalizedScoreResponse.NormalizedExamScore> normalizedScores = studentResults.stream()
                .map(result -> {
                    List<ExamResult> allResults = resultRepository.findByExamId(result.getExam().getId());
                    List<Integer> allScores = allResults.stream()
                            .map(ExamResult::getTotalScore)
                            .toList();

                    double mean = statisticsCalculator.calculateMean(allScores);
                    double stdDev = statisticsCalculator.calculateStandardDeviation(allScores);
                    double zScore = statisticsCalculator.calculateZScore(result.getTotalScore(), mean, stdDev);
                    double normalizedScore = statisticsCalculator.calculateNormalizedScore(zScore);

                    return new NormalizedScoreResponse.NormalizedExamScore(
                            result.getExam().getId(),
                            result.getExam().getName(),
                            result.getCreatedAt(),
                            result.getTotalScore(),
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

    /**
     * 선지별 선택률 분석
     */
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
                        count,
                        Math.round(percentage * 10) / 10.0,
                        choice.equals(correctAnswer)
                ));
            }

            List<ChoiceDistributionResponse.AttractiveDistractor> attractiveDistractors = choiceCounts.entrySet().stream()
                    .filter(e -> !e.getKey().equals(correctAnswer) && !e.getKey().equals("NO_ANSWER"))
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .limit(2)
                    .map(e -> new ChoiceDistributionResponse.AttractiveDistractor(
                            e.getKey(),
                            e.getValue(),
                            Math.round(statisticsCalculator.calculateChoiceRate(e.getValue(), totalParticipants) * 10) / 10.0
                    ))
                    .toList();

            questions.add(new ChoiceDistributionResponse.QuestionChoiceDistribution(
                    qNum,
                    questionType,
                    correctAnswer,
                    correctCount,
                    Math.round(correctRate * 10) / 10.0,
                    choiceDistribution,
                    attractiveDistractors
            ));
        }

        return new ChoiceDistributionResponse(examId, exam.getName(), totalParticipants, questions);
    }

    /**
     * 대시보드 통계 조회 (유형별 정답률, 오답률 TOP 10)
     */
    public DashboardStatisticsResponse getDashboardStatistics() {
        log.debug("Getting dashboard statistics");

        List<Exam> allExams = examRepository.findAll();
        List<ExamResult> allResults = new ArrayList<>();
        for (Exam exam : allExams) {
            allResults.addAll(resultRepository.findByExamId(exam.getId()));
        }

        if (allResults.isEmpty()) {
            return new DashboardStatisticsResponse(
                    allExams.size(), 0, 0.0,
                    Collections.emptyList(), Collections.emptyList()
            );
        }

        // 전체 평균 점수
        double overallAverage = allResults.stream()
                .mapToInt(ExamResult::getTotalScore)
                .average()
                .orElse(0.0);

        // 유형별 정답률 계산
        Map<String, int[]> typeStats = new HashMap<>(); // [correct, total]
        // 문항별 오답 정보
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

                // 문항별 오답 집계
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

        // 유형별 정답률 리스트
        List<DashboardStatisticsResponse.TypeAccuracy> typeAccuracyList = typeStats.entrySet().stream()
                .map(entry -> {
                    int correct = entry.getValue()[0];
                    int total = entry.getValue()[1];
                    double accuracy = total > 0 ? (double) correct / total * 100 : 0;
                    return new DashboardStatisticsResponse.TypeAccuracy(
                            entry.getKey(),
                            total,
                            correct,
                            Math.round(accuracy * 10) / 10.0
                    );
                })
                .sorted((a, b) -> Double.compare(b.accuracyRate(), a.accuracyRate()))
                .toList();

        // 오답률 TOP 10
        List<DashboardStatisticsResponse.TopIncorrectQuestion> topIncorrect = questionErrors.values().stream()
                .filter(info -> info.totalAttempts >= 5) // 최소 5명 이상 응시한 문항만
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
                            info.exam.getId(),
                            info.exam.getName(),
                            info.questionNumber,
                            info.exam.getQuestionType(info.questionNumber),
                            info.exam.getCorrectAnswerForQuestion(info.questionNumber),
                            info.totalAttempts,
                            info.incorrectCount,
                            Math.round(incorrectRate * 10) / 10.0,
                            topWrongChoice,
                            Math.round(topWrongRate * 10) / 10.0
                    );
                })
                .toList();

        return new DashboardStatisticsResponse(
                allExams.size(),
                allResults.size(),
                Math.round(overallAverage * 10) / 10.0,
                typeAccuracyList,
                topIncorrect
        );
    }

    // Helper class for question error tracking
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

    /**
     * 목표 등급 시뮬레이션
     */
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
                studentId,
                currentGrade,
                targetGrade,
                currentScore,
                targetScore,
                Math.max(0, scoreDifference),
                achievable,
                scenarios
        );
    }

    // === Private helper methods ===

    private List<ExamStatisticsResponse.TypeAccuracy> calculateExamTypeAccuracy(Exam exam, List<ExamResult> results) {
        if (exam.getQuestionTypes() == null) {
            return Collections.emptyList();
        }

        Map<String, int[]> typeStats = new HashMap<>(); // [correct, total]

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
                            entry.getKey(),
                            total,
                            correct,
                            Math.round(accuracy * 10) / 10.0
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

            // 가장 많이 선택된 오답 찾기
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
                    qNum,
                    questionType,
                    correctAnswer,
                    totalParticipants,
                    incorrectCount,
                    Math.round(incorrectRate * 10) / 10.0,
                    topWrongChoice,
                    Math.round(topWrongRate * 10) / 10.0
            ));
        }

        // 오답률 기준 정렬 후 TOP 10
        return topIncorrect.stream()
                .filter(q -> q.incorrectCount() > 0)
                .sorted((a, b) -> Double.compare(b.incorrectRate(), a.incorrectRate()))
                .limit(10)
                .toList();
    }

    // P/NP 시험 통계를 위한 헬퍼 클래스
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

        return new PassFailStats(
                Math.round(passRate * 10) / 10.0,
                passCount,
                failCount
        );
    }

    private List<ExamStatisticsResponse.GradeDistribution> calculateGradeDistribution(Exam exam, List<ExamResult> results) {
        List<Integer> allScores = results.stream()
                .map(ExamResult::getTotalScore)
                .toList();

        GradingType gradingType = exam.getGradingType() != null ? exam.getGradingType() : GradingType.ABSOLUTE;
        GradeScale gradeScale = exam.getGradeScale() != null ? exam.getGradeScale() : GradeScale.NINE_GRADE;

        Map<Integer, Long> gradeCounts = results.stream()
                .map(r -> {
                    // 상대평가는 동적 계산, 그 외는 DB값 사용
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
                        entry.getKey(),
                        entry.getValue().intValue(),
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
                        entry.getKey(),
                        entry.getValue(),
                        Math.round((double) entry.getValue() / total * 1000) / 10.0
                ))
                .toList();
    }

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
                            type,
                            correct,
                            total,
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
                            type,
                            correct,
                            total,
                            Math.round(currentAccuracy * 10) / 10.0,
                            additionalNeeded,
                            Math.round(targetAccuracy * 10) / 10.0,
                            potentialGain,
                            priority
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

    /**
     * 학원별 성취도 비교
     */
    public List<AcademyComparisonResponse> getAcademyComparison(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamException(ExamErrorCode.EXAM_NOT_FOUND));

        // 절대평가인 경우 기존 쿼리 사용 (DB에 저장된 grade 값 활용)
        if (exam.getCategory() != ExamCategory.GRADED ||
            exam.getGradingType() != GradingType.RELATIVE) {
            List<AcademyComparisonProjection> projections =
                    resultRepository.findAcademyComparisonByExamId(examId);

            return projections.stream()
                    .map(p -> new AcademyComparisonResponse(
                            p.getAcademyId(),
                            p.getAcademyName(),
                            p.getParticipantCount(),
                            Math.round(p.getAverage() * 10) / 10.0,
                            p.getGrade1Count(),
                            p.getParticipantCount() > 0
                                    ? Math.round((double) p.getGrade1Count() / p.getParticipantCount() * 1000) / 10.0
                                    : 0.0
                    ))
                    .toList();
        }

        // 상대평가인 경우 동적으로 등급 계산
        return calculateAcademyComparisonForRelativeGrading(exam);
    }

    /**
     * 상대평가용 학원별 성취도 비교 (등급 동적 계산)
     */
    private List<AcademyComparisonResponse> calculateAcademyComparisonForRelativeGrading(Exam exam) {
        var resultsWithAcademy = resultRepository.findResultsWithAcademyByExamId(exam.getId());

        if (resultsWithAcademy.isEmpty()) {
            return Collections.emptyList();
        }

        // 전체 점수 목록 (상대평가 등급 계산용)
        List<Integer> allScores = resultsWithAcademy.stream()
                .map(r -> r.getTotalScore())
                .toList();

        GradeScale gradeScale = exam.getGradeScale() != null ? exam.getGradeScale() : GradeScale.NINE_GRADE;

        // 학원별로 그룹화하여 계산
        Map<Long, List<com.lumie.exam.adapter.out.persistence.ResultWithAcademyProjection>> byAcademy =
                resultsWithAcademy.stream()
                        .collect(Collectors.groupingBy(r -> r.getAcademyId()));

        return byAcademy.entrySet().stream()
                .map(entry -> {
                    Long academyId = entry.getKey();
                    var academyResults = entry.getValue();
                    String academyName = academyResults.get(0).getAcademyName();

                    int participantCount = academyResults.size();
                    double average = academyResults.stream()
                            .mapToInt(r -> r.getTotalScore())
                            .average()
                            .orElse(0.0);

                    // 상대평가 등급 계산하여 1등급 수 카운트
                    long grade1Count = academyResults.stream()
                            .filter(r -> {
                                int grade = gradeCalculator.calculateRelativeGrade(
                                        r.getTotalScore(), allScores, gradeScale);
                                return grade == 1;
                            })
                            .count();

                    double grade1Percentage = participantCount > 0
                            ? Math.round((double) grade1Count / participantCount * 1000) / 10.0
                            : 0.0;

                    return new AcademyComparisonResponse(
                            academyId,
                            academyName,
                            participantCount,
                            Math.round(average * 10) / 10.0,
                            (int) grade1Count,
                            grade1Percentage
                    );
                })
                .sorted(Comparator.comparingDouble(AcademyComparisonResponse::average).reversed())
                .toList();
    }
}
