package com.lumie.exam.application.service;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.application.dto.response.statistics.StudentGradeResponse;
import com.lumie.exam.application.dto.response.statistics.StudentResultSummaryResponse;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.student.api.StudentService;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.exception.ExamException;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StudentGradeQueryService {

    private final ExamRepository examRepository;
    private final ExamResultRepository resultRepository;
    private final StatisticsCalculator statisticsCalculator;
    private final GradeCalculator gradeCalculator;
    private final StudentService studentService;

    /**
     * Returns a page of student grade responses for the given exam.
     *
     * <p>Honors {@code pageable.getSort()} over the allowlist {@code {rank, totalScore}};
     * default {@code rank ASC} when no sort is specified. Rank is computed in-memory from
     * {@link ExamResult} rows because it is a derived value, not a DB column.
     *
     * <p>Both paged and unpaged {@link Pageable} instances are supported. When
     * {@link Pageable#isUnpaged()} is {@code true}, the full sorted list is returned as a
     * {@link PageImpl} with {@link Pageable#unpaged()} so that {@code Page<T>} consumers
     * (e.g. inter-module callers via {@code libs/internal-api}) receive a valid page without
     * triggering {@link UnsupportedOperationException} from {@link Pageable#getOffset()}.
     *
     * <p>For {@link ExamCategory#PASS_FAIL} exams the {@code isPassed} flag is applied as a
     * secondary tiebreaker after the primary sort field, preserving the conventional view of
     * passed students above failed ones when ranks or scores are equal.
     */
    public Page<StudentGradeResponse> getStudentGrades(Long examId, Pageable pageable) {
        log.debug("Getting student grades for exam: {}", examId);

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamException(ExamErrorCode.EXAM_NOT_FOUND));

        List<ExamResult> results = resultRepository.findByExamId(examId);
        Map<Long, StudentService.StudentData> studentMap = fetchStudentMap(results);

        List<Integer> allScores = results.stream()
                .map(ExamResult::getTotalScore)
                .toList();

        GradeScale gradeScale = exam.getGradeScale() != null ? exam.getGradeScale() : GradeScale.NINE_GRADE;

        List<StudentGradeResponse> grades = results.stream()
                .map(result -> {
                    StudentService.StudentData student = resolveStudent(studentMap, result);
                    int rank = statisticsCalculator.calculateRank(result.getTotalScore(), allScores);
                    double percentile = statisticsCalculator.calculatePercentile(result.getTotalScore(), allScores);

                    Integer grade = result.getGrade();
                    if (exam.getCategory() == ExamCategory.GRADED &&
                        exam.getGradingType() == GradingType.RELATIVE) {
                        grade = gradeCalculator.calculateRelativeGrade(result.getTotalScore(), allScores, gradeScale);
                    }

                    return new StudentGradeResponse(
                            result.getStudentId(),
                            student.studentPhone(),
                            result.getStudentId() != null,
                            student.studentName(),
                            result.getTotalScore(),
                            rank,
                            Math.round(percentile * 10) / 10.0,
                            grade,
                            exam.getCategory(),
                            result.isPassed(),
                            result.getCreatedAt(),
                            result.getId()
                    );
                })
                .sorted(buildComparator(pageable.getSort(), exam.getCategory()))
                .toList();

        if (!pageable.isPaged()) {
            return new PageImpl<>(grades, Pageable.unpaged(), grades.size());
        }

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), grades.size());
        List<StudentGradeResponse> pageContent = grades.subList(start, end);

        return new PageImpl<>(pageContent, pageable, grades.size());
    }

    /**
     * Builds a {@link Comparator} that honours the first {@link Sort.Order} from the given
     * {@link Sort}. Allowed primary fields are {@code rank} (default ASC — lower is better) and
     * {@code totalScore} (default DESC — higher is better). Any other / absent field falls back to
     * {@code rank ASC} — the incoming direction is ignored for unknown fields so that the
     * {@code SortFieldAllowlistInterceptor} bypass path always produces a predictable, best-first
     * result rather than silently inheriting the caller-supplied direction.
     *
     * <p>For {@link ExamCategory#PASS_FAIL} exams, a secondary tiebreaker on {@code isPassed}
     * (passed first) followed by {@code score} descending is appended after the primary key.
     */
    private static Comparator<StudentGradeResponse> buildComparator(Sort sort, ExamCategory category) {
        Sort.Order order = sort.iterator().hasNext() ? sort.iterator().next() : null;

        String field = (order != null) ? order.getProperty() : "rank";

        Comparator<StudentGradeResponse> primary;
        if ("totalScore".equals(field)) {
            // "totalScore" in the allowlist maps to the DTO's `score` component.
            // Direction is honoured literally: ASC = lowest score first, DESC = highest score first.
            boolean ascending = (order != null) && order.getDirection().isAscending();
            primary = ascending
                    ? Comparator.comparingInt(StudentGradeResponse::score)
                    : Comparator.comparingInt(StudentGradeResponse::score).reversed();
        } else if ("rank".equals(field)) {
            // "rank": lower rank number = better, so ASC = best-rank-first.
            // Direction is honoured literally for the known "rank" field.
            boolean ascending = (order == null) || order.getDirection().isAscending();
            primary = ascending
                    ? Comparator.comparingInt(StudentGradeResponse::rank)
                    : Comparator.comparingInt(StudentGradeResponse::rank).reversed();
        } else {
            // Unknown field (should be rejected by SortFieldAllowlistInterceptor before reaching
            // here, but this code path exists as a safety net). Always fall back to rank ASC
            // regardless of the incoming direction — principle of least surprise; Javadoc contract.
            primary = Comparator.comparingInt(StudentGradeResponse::rank);
        }

        if (category == ExamCategory.PASS_FAIL) {
            // Secondary tiebreaker for pass/fail exams: passed students before failed (reversed
            // because Boolean natural order is false < true), then score descending within each
            // pass/fail group. This preserves the behaviour that was hardcoded before this change
            // for the default (rank ASC) case.
            Comparator<StudentGradeResponse> passFirst =
                    Comparator.comparing(StudentGradeResponse::isPassed).reversed();
            Comparator<StudentGradeResponse> scoreDesc =
                    Comparator.comparingInt(StudentGradeResponse::score).reversed();
            return primary.thenComparing(passFirst).thenComparing(scoreDesc);
        }

        return primary;
    }

    public List<StudentResultSummaryResponse> getStudentResultSummaries(Long examId) {
        log.debug("Getting student result summaries for exam: {}", examId);

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamException(ExamErrorCode.EXAM_NOT_FOUND));

        List<ExamResult> results = resultRepository.findByExamId(examId);
        Map<Long, StudentService.StudentData> studentMap = fetchStudentMap(results);

        return results.stream()
                .map(result -> {
                    StudentService.StudentData student = resolveStudent(studentMap, result);
                    return new StudentResultSummaryResponse(
                            result.getId(),
                            result.getStudentId(),
                            result.getPhoneNumber(),
                            result.getStudentId() != null,
                            student.studentName(),
                            student.isActive(),
                            result.getTotalScore(),
                            result.getGrade(),
                            exam.getCategory(),
                            result.isPassed()
                    );
                })
                .sorted(Comparator.comparing(StudentResultSummaryResponse::studentName))
                .toList();
    }

    private Map<Long, StudentService.StudentData> fetchStudentMap(List<ExamResult> results) {
        List<Long> studentIds = results.stream()
                .map(ExamResult::getStudentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (studentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        return studentService.getStudentsByIds(tenantSlug, studentIds).students().stream()
                .collect(java.util.stream.Collectors.toMap(StudentService.StudentData::id, s -> s));
    }

    private static StudentService.StudentData resolveStudent(
            Map<Long, StudentService.StudentData> studentMap, ExamResult result) {
        if (result.getStudentId() == null) {
            String displayName = result.getPhoneNumber() != null
                    ? result.getPhoneNumber() : "미등록 학생";
            return new StudentService.StudentData(
                    null, null, displayName, result.getPhoneNumber(), false, null, null);
        }
        return studentMap.getOrDefault(result.getStudentId(),
                new StudentService.StudentData(
                        result.getStudentId(), null, "학생 " + result.getStudentId(),
                        null, true, null, null));
    }
}
