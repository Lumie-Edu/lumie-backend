package com.lumie.exam.application.service;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.application.dto.response.statistics.ClassComparisonResponse;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.classroom.api.ClassService;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.common.exception.BusinessException;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.vo.ExamCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Per-class breakdown of an exam: how each enrolled class performed
 * compared to the others (mean, std dev, top/bottom, grade buckets).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ClassComparisonQueryService {

    private static final Long UNASSIGNED_CLASS_ID = null;
    private static final String UNASSIGNED_CLASS_NAME = "미배정";

    private final ExamRepository examRepository;
    private final ExamResultRepository resultRepository;
    private final StatisticsCalculator statisticsCalculator;
    private final GradeCalculator gradeCalculator;
    private final ClassService classService;
    private final ExamResultCohortResolver cohortResolver;

    public ClassComparisonResponse getClassComparison(Long examId) {
        return getClassComparison(examId, null, null, null);
    }

    public ClassComparisonResponse getClassComparison(
            Long examId,
            Long classId,
            Boolean hasActiveEnrollment,
            Boolean isActive) {
        log.debug("Getting class comparison for exam: {}", examId);

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.EXAM_NOT_FOUND));

        List<ExamResult> allResults = resultRepository.findByExamId(examId);
        List<ExamResult> results = filterResults(allResults, classId, hasActiveEnrollment, isActive);
        if (results.isEmpty()) {
            return new ClassComparisonResponse(
                    examId, exam.getName(), exam.getCategory(), 0, Collections.emptyList()
            );
        }

        Set<Long> studentIds = results.stream()
                .map(ExamResult::getStudentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        String tenantSlug = TenantContextHolder.getRequiredTenant();
        Map<Long, List<ClassService.EnrolledClassData>> classesByStudent = classService
                .getEnrolledClassesForStudents(tenantSlug, studentIds).stream()
                .collect(Collectors.groupingBy(ClassService.EnrolledClassData::studentId));

        Map<Long, String> classNames = new HashMap<>();
        Map<Long, List<ExamResult>> resultsByClass = new LinkedHashMap<>();
        List<ExamResult> unassigned = new ArrayList<>();

        for (ExamResult result : results) {
            Long studentId = result.getStudentId();
            List<ClassService.EnrolledClassData> memberships =
                    studentId != null ? classesByStudent.get(studentId) : null;

            if (memberships != null && classId != null) {
                memberships = memberships.stream()
                        .filter(membership -> classId.equals(membership.classId()))
                        .toList();
            }

            if (memberships == null || memberships.isEmpty()) {
                unassigned.add(result);
                continue;
            }
            for (ClassService.EnrolledClassData membership : memberships) {
                classNames.putIfAbsent(membership.classId(), membership.className());
                resultsByClass.computeIfAbsent(membership.classId(), k -> new ArrayList<>()).add(result);
            }
        }

        List<Integer> allScores = allResults.stream().map(ExamResult::getTotalScore).toList();
        List<ClassComparisonResponse.ClassMetric> classes = new ArrayList<>();
        resultsByClass.entrySet().stream()
                .sorted(Comparator.comparing(e -> classNames.getOrDefault(e.getKey(), "")))
                .forEach(e -> classes.add(buildClassMetric(
                        e.getKey(), classNames.get(e.getKey()), e.getValue(), exam, allScores
                )));
        if (!unassigned.isEmpty()) {
            classes.add(buildClassMetric(UNASSIGNED_CLASS_ID, UNASSIGNED_CLASS_NAME, unassigned, exam, allScores));
        }

        return new ClassComparisonResponse(
                examId, exam.getName(), exam.getCategory(), results.size(), classes
        );
    }

    private ClassComparisonResponse.ClassMetric buildClassMetric(
            Long classId, String className, List<ExamResult> groupResults, Exam exam, List<Integer> allScores) {
        List<Integer> scores = groupResults.stream().map(ExamResult::getTotalScore).toList();
        double mean = statisticsCalculator.calculateMean(scores);
        double stddev = statisticsCalculator.calculateStandardDeviation(scores);
        int highest = scores.stream().max(Integer::compareTo).orElse(0);
        int lowest = scores.stream().min(Integer::compareTo).orElse(0);

        Double grade1Ratio = null;
        Double passRate = null;
        List<ClassComparisonResponse.GradeBucket> buckets = Collections.emptyList();

        if (exam.getCategory() == ExamCategory.PASS_FAIL) {
            long pass = groupResults.stream().filter(ExamResult::isPassed).count();
            passRate = Math.round((double) pass / groupResults.size() * 1000) / 10.0;
        } else {
            buckets = computeGradeBuckets(exam, groupResults, allScores);
            int grade1Count = buckets.stream().filter(b -> b.grade() == 1).mapToInt(ClassComparisonResponse.GradeBucket::count).sum();
            grade1Ratio = Math.round((double) grade1Count / groupResults.size() * 1000) / 10.0;
        }

        return new ClassComparisonResponse.ClassMetric(
                classId, className, groupResults.size(),
                Math.round(mean * 10) / 10.0,
                Math.round(stddev * 10) / 10.0,
                highest, lowest,
                grade1Ratio, passRate, buckets
        );
    }

    private List<ClassComparisonResponse.GradeBucket> computeGradeBuckets(
            Exam exam, List<ExamResult> groupResults, List<Integer> allScoresInExam) {
        // 등급 산출은 시험 전체 기준(상대평가의 경우)이지만, 분포 자체는 그룹 인원 비중으로 계산
        Map<Integer, Long> gradeCounts = groupResults.stream()
                .map(r -> gradeCalculator.resolveStoredOrRelativeGrade(exam, r, allScoresInExam))
                .filter(g -> g != null && g > 0)
                .collect(Collectors.groupingBy(g -> g, Collectors.counting()));

        return gradeCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new ClassComparisonResponse.GradeBucket(
                        e.getKey(), e.getValue().intValue(),
                        Math.round((double) e.getValue() / groupResults.size() * 1000) / 10.0
                ))
                .toList();
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
}
