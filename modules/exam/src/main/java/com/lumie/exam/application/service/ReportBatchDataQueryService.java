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
import com.lumie.exam.domain.vo.GradingType;
import com.lumie.student.api.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportBatchDataQueryService {

    public static final int MAX_BATCH_SIZE = 30;

    private final ExamRepository examRepository;
    private final ExamResultRepository resultRepository;
    private final ReportGenerationJobRepository jobRepository;
    private final ExamSummaryStatisticsService examSummaryStatisticsService;
    private final StudentService studentService;
    private final StatisticsCalculator statisticsCalculator;
    private final GradeCalculator gradeCalculator;

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
        Exam exam = jobId == null
                ? getExam(examId)
                : validateJobAndGetExam(jobId, examId, distinctStudentIds);
        BatchContext context = buildContext(exam, distinctStudentIds);

        List<ReportBatchDataResponse.Item> items = studentIds.stream()
                .map(studentId -> buildItem(context.exam(), studentId, context.resultByStudentId(),
                        context.studentById(), context.stabilityByStudentId(), context.rankContext()))
                .toList();

        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
        log.info("Built report batch data: examId={}, jobId={}, students={}, results={}, elapsedMs={}",
                examId, jobId, studentIds.size(), context.resultByStudentId().size(), elapsedMillis);

        return new ReportBatchDataResponse(context.examStatistics(), items);
    }

    private Exam getExam(Long examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.EXAM_NOT_FOUND));
    }

    private Exam validateJobAndGetExam(Long jobId, Long examId, List<Long> studentIds) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        ReportGenerationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.RESULT_NOT_FOUND));
        if (!Objects.equals(job.getTenantSlug(), tenantSlug)) {
            throw new BusinessException(ExamErrorCode.INVALID_EXAM_DATA, "jobId does not belong to tenant");
        }
        Exam exam = job.getExam();
        if (!Objects.equals(exam.getId(), examId)) {
            throw new BusinessException(ExamErrorCode.INVALID_EXAM_DATA, "jobId does not belong to examId");
        }
        if (!job.getStudentIds().containsAll(studentIds)) {
            throw new BusinessException(ExamErrorCode.INVALID_EXAM_DATA, "studentIds do not belong to jobId");
        }
        return exam;
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
                ExamResultResponse.from(result, student.studentName(),
                        gradeCalculator.resolveStoredOrRelativeGrade(exam, result, rankContext.allScores())),
                studentRank,
                stability,
                questionResults
        );
    }

    private RankContext buildRankContext(Exam exam, List<ExamResult> examResults) {
        List<Integer> allScores = examResults.stream()
                .map(ExamResult::getTotalScore)
                .toList();
        Map<String, List<Double>> typeAccuracies = ExamStatisticsProjection.calculateTypeAccuracies(exam, examResults);
        return new RankContext(allScores, typeAccuracies, examResults.size());
    }

    private StudentRankResponse calculateStudentRank(Exam exam, ExamResult result, RankContext context) {
        int rank = statisticsCalculator.calculateRank(result.getTotalScore(), context.allScores());
        double percentile = statisticsCalculator.calculatePercentile(result.getTotalScore(), context.allScores());

        Integer grade = gradeCalculator.resolveStoredOrRelativeGrade(exam, result, context.allScores());

        return new StudentRankResponse(
                result.getStudentId(),
                exam.getId(),
                exam.getName(),
                result.getTotalScore(),
                rank,
                context.totalParticipants(),
                Math.round(percentile * 10) / 10.0,
                grade,
                ExamStatisticsProjection.calculateTypePercentiles(exam, result, context.typeAccuracies())
        );
    }







    private Map<Long, StabilityIndexResponse> calculateStabilityIndexes(List<Long> studentIds) {
        if (studentIds.isEmpty()) {
            return Map.of();
        }

        List<ExamResult> studentResults = resultRepository.findByStudentIdIn(studentIds);
        List<Long> relativeExamIds = studentResults.stream()
                .filter(result -> result.getExam().getCategory() == ExamCategory.GRADED
                        && result.getExam().getGradingType() == GradingType.RELATIVE)
                .map(result -> result.getExam().getId())
                .distinct()
                .toList();
        Map<Long, List<Integer>> relativeScoresByExamId = relativeExamIds.isEmpty()
                ? Map.of()
                : resultRepository.findByExamIdIn(relativeExamIds).stream()
                        .collect(Collectors.groupingBy(
                                result -> result.getExam().getId(),
                                Collectors.mapping(ExamResult::getTotalScore, Collectors.toList())));

        return studentResults.stream()
                .collect(Collectors.groupingBy(ExamResult::getStudentId))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> ExamStatisticsProjection.calculateStabilityIndex(
                                entry.getKey(), entry.getValue(), statisticsCalculator,
                                gradeCalculator, relativeScoresByExamId)
                ));
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

}
