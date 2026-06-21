package com.lumie.exam.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.application.dto.response.ExamResultResponse;
import com.lumie.exam.application.dto.response.QuestionResultResponse;
import com.lumie.exam.application.dto.response.ReportBatchDataResponse;
import com.lumie.exam.application.dto.response.statistics.ExamStatisticsResponse;
import com.lumie.exam.application.dto.response.statistics.StabilityIndexResponse;
import com.lumie.exam.application.dto.response.statistics.StudentRankResponse;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.entity.QuestionResult;
import com.lumie.exam.domain.entity.ReportGenerationJob;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.repository.ReportGenerationJobRepository;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import com.lumie.student.api.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportBatchDataQueryService {

    public static final int MAX_BATCH_SIZE = 30;
    private static final long JOB_CONTEXT_TTL_NANOS = 5L * 60 * 1_000_000_000L;
    private static final int MAX_CACHED_JOB_CONTEXTS = 128;

    private final ExamRepository examRepository;
    private final ExamResultRepository resultRepository;
    private final ReportGenerationJobRepository jobRepository;
    private final ExamSummaryStatisticsService examSummaryStatisticsService;
    private final StudentService studentService;
    private final StatisticsCalculator statisticsCalculator;
    private final GradeCalculator gradeCalculator;
    private final ConcurrentMap<JobContextKey, CachedBatchContext> jobContextCache = new ConcurrentHashMap<>();

    public ReportBatchDataResponse getBatchData(Long examId, List<Long> studentIds) {
        return getBatchData(examId, studentIds, null);
    }

    public ReportBatchDataResponse getBatchData(Long examId, List<Long> studentIds, Long jobId) {
        validateStudentIds(studentIds);

        List<Long> distinctStudentIds = studentIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        long startedAt = System.nanoTime();
        BatchContext context = jobId == null
                ? buildUncachedContext(examId, distinctStudentIds)
                : getCachedJobContext(jobId, examId).context();

        List<ReportBatchDataResponse.Item> items = studentIds.stream()
                .map(studentId -> buildItem(context.exam(), studentId, context.resultByStudentId(),
                        context.studentById(), context.stabilityByStudentId(), context.rankContext()))
                .toList();

        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
        log.info("Built report batch data: examId={}, jobId={}, students={}, results={}, elapsedMs={}",
                examId, jobId, studentIds.size(), context.resultByStudentId().size(), elapsedMillis);

        return new ReportBatchDataResponse(context.examStatistics(), items);
    }

    private BatchContext buildUncachedContext(Long examId, List<Long> studentIds) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.EXAM_NOT_FOUND));
        return buildContext(exam, studentIds);
    }

    private CachedBatchContext getCachedJobContext(Long jobId, Long examId) {
        purgeExpiredJobContexts();
        long now = System.nanoTime();
        JobContextKey cacheKey = new JobContextKey(TenantContextHolder.getRequiredTenant(), jobId, examId);
        CachedBatchContext cached = jobContextCache.get(cacheKey);
        if (cached != null && !cached.isExpired(now)) {
            return cached;
        }

        CachedBatchContext refreshed = new CachedBatchContext(buildJobContext(cacheKey, examId), now);
        jobContextCache.put(cacheKey, refreshed);
        return refreshed;
    }

    private void purgeExpiredJobContexts() {
        long now = System.nanoTime();
        jobContextCache.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
        if (jobContextCache.size() > MAX_CACHED_JOB_CONTEXTS) {
            jobContextCache.clear();
        }
    }

    private BatchContext buildJobContext(JobContextKey cacheKey, Long examId) {
        ReportGenerationJob job = jobRepository.findById(cacheKey.jobId())
                .orElseThrow(() -> new BusinessException(ExamErrorCode.RESULT_NOT_FOUND));
        if (!Objects.equals(job.getTenantSlug(), cacheKey.tenantSlug())) {
            throw new BusinessException(ExamErrorCode.INVALID_EXAM_DATA, "jobId does not belong to tenant");
        }
        Exam exam = job.getExam();
        if (!Objects.equals(exam.getId(), examId)) {
            throw new BusinessException(ExamErrorCode.INVALID_EXAM_DATA, "jobId does not belong to examId");
        }
        return buildContext(exam, List.copyOf(job.getStudentIds()));
    }

    private BatchContext buildContext(Exam exam, List<Long> contextStudentIds) {
        List<Long> distinctStudentIds = contextStudentIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<ExamResult> examResults = resultRepository.findByExamId(exam.getId());
        ExamStatisticsResponse examStatistics = examSummaryStatisticsService.buildExamStatistics(exam, examResults);
        Map<Long, ExamResult> resultByStudentId = examResults.stream()
                .filter(r -> r.getStudentId() != null)
                .collect(Collectors.toMap(
                        ExamResult::getStudentId,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
        Map<Long, StudentService.StudentData> studentById = fetchStudents(distinctStudentIds);
        Map<Long, StabilityIndexResponse> stabilityByStudentId = calculateStabilityIndexes(distinctStudentIds);
        RankContext rankContext = buildRankContext(exam, examResults);
        return new BatchContext(
                exam,
                examStatistics,
                resultByStudentId,
                studentById,
                stabilityByStudentId,
                rankContext
        );
    }

    private void validateStudentIds(List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            throw new BusinessException(ExamErrorCode.INVALID_EXAM_DATA, "studentIds must not be empty");
        }
        if (studentIds.size() > MAX_BATCH_SIZE) {
            throw new BusinessException(
                    ExamErrorCode.INVALID_EXAM_DATA,
                    "studentIds size must be less than or equal to " + MAX_BATCH_SIZE);
        }
        if (studentIds.stream().anyMatch(Objects::isNull)) {
            throw new BusinessException(ExamErrorCode.INVALID_EXAM_DATA, "studentIds must not contain null");
        }
    }

    private Map<Long, StudentService.StudentData> fetchStudents(List<Long> studentIds) {
        if (studentIds.isEmpty()) {
            return Map.of();
        }
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        return studentService.getStudentsByIds(tenantSlug, studentIds).students().stream()
                .collect(Collectors.toMap(
                        StudentService.StudentData::id,
                        Function.identity(),
                        (first, ignored) -> first
                ));
    }

    private ReportBatchDataResponse.Item buildItem(
            Exam exam,
            Long studentId,
            Map<Long, ExamResult> resultByStudentId,
            Map<Long, StudentService.StudentData> studentById,
            Map<Long, StabilityIndexResponse> stabilityByStudentId,
            RankContext rankContext
    ) {
        StudentService.StudentData student = studentById.get(studentId);
        if (student == null) {
            return ReportBatchDataResponse.Item.failure(studentId, "STUDENT_NOT_FOUND");
        }

        ExamResult result = resultByStudentId.get(studentId);
        if (result == null) {
            return ReportBatchDataResponse.Item.failure(studentId, "RESULT_NOT_FOUND");
        }

        StudentRankResponse studentRank = exam.getCategory() == ExamCategory.PASS_FAIL
                ? null
                : calculateStudentRank(exam, result, rankContext);
        StabilityIndexResponse stability = stabilityByStudentId.getOrDefault(
                studentId,
                new StabilityIndexResponse(studentId, 0, 0.0, 0.0, 0.0, "N/A", List.of())
        );
        List<QuestionResultResponse> questionResults = result.getQuestionResults().stream()
                .sorted(Comparator.comparing(QuestionResult::getQuestionNumber))
                .map(qr -> QuestionResultResponse.from(qr, exam))
                .toList();

        return ReportBatchDataResponse.Item.success(
                studentId,
                new ReportBatchDataResponse.StudentInfo(student.id(), student.studentName(), student.studentPhone()),
                ExamResultResponse.from(result, student.studentName()),
                studentRank,
                stability,
                questionResults
        );
    }

    private RankContext buildRankContext(Exam exam, List<ExamResult> examResults) {
        List<Integer> allScores = examResults.stream()
                .map(ExamResult::getTotalScore)
                .toList();
        Map<String, List<Double>> typeAccuracies = calculateTypeAccuracies(exam, examResults);
        return new RankContext(allScores, typeAccuracies, examResults.size());
    }

    private StudentRankResponse calculateStudentRank(Exam exam, ExamResult result, RankContext context) {
        int rank = statisticsCalculator.calculateRank(result.getTotalScore(), context.allScores());
        double percentile = statisticsCalculator.calculatePercentile(result.getTotalScore(), context.allScores());

        Integer grade = result.getGrade();
        if (exam.getCategory() == ExamCategory.GRADED && exam.getGradingType() == GradingType.RELATIVE) {
            GradeScale gradeScale = exam.getGradeScale() != null ? exam.getGradeScale() : GradeScale.NINE_GRADE;
            grade = gradeCalculator.calculateRelativeGrade(result.getTotalScore(), context.allScores(), gradeScale);
        }

        return new StudentRankResponse(
                result.getStudentId(),
                exam.getId(),
                exam.getName(),
                result.getTotalScore(),
                rank,
                context.totalParticipants(),
                Math.round(percentile * 10) / 10.0,
                grade,
                calculateTypePercentiles(exam, result, context.typeAccuracies())
        );
    }

    private Map<String, List<Double>> calculateTypeAccuracies(Exam exam, List<ExamResult> examResults) {
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

    private List<StudentRankResponse.TypePercentile> calculateTypePercentiles(
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

    private Map<String, int[]> calculateTypeStats(Exam exam, Collection<QuestionResult> questionResults) {
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

    private Map<Long, StabilityIndexResponse> calculateStabilityIndexes(List<Long> studentIds) {
        if (studentIds.isEmpty()) {
            return Map.of();
        }

        return resultRepository.findByStudentIdIn(studentIds).stream()
                .collect(Collectors.groupingBy(ExamResult::getStudentId))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> calculateStabilityIndex(entry.getKey(), entry.getValue())
                ));
    }

    private StabilityIndexResponse calculateStabilityIndex(Long studentId, List<ExamResult> results) {
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
                statisticsCalculator.determineStabilityLevel(cv),
                history
        );
    }

    private record RankContext(
            List<Integer> allScores,
            Map<String, List<Double>> typeAccuracies,
            int totalParticipants
    ) {
    }

    private record BatchContext(
            Exam exam,
            ExamStatisticsResponse examStatistics,
            Map<Long, ExamResult> resultByStudentId,
            Map<Long, StudentService.StudentData> studentById,
            Map<Long, StabilityIndexResponse> stabilityByStudentId,
            RankContext rankContext
    ) {
    }

    private record CachedBatchContext(BatchContext context, long createdAtNanos) {
        boolean isExpired(long nowNanos) {
            return nowNanos - createdAtNanos > JOB_CONTEXT_TTL_NANOS;
        }
    }

    private record JobContextKey(String tenantSlug, Long jobId, Long examId) {
    }
}
