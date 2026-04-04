package com.lumie.exam.application.service;

import com.lumie.exam.application.dto.response.statistics.StudentGradeResponse;
import com.lumie.exam.application.dto.response.statistics.StudentResultSummaryResponse;
import com.lumie.exam.application.port.out.StudentServicePort;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
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

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StudentGradeQueryService {

    private final ExamRepository examRepository;
    private final ExamResultRepository resultRepository;
    private final StatisticsCalculator statisticsCalculator;
    private final GradeCalculator gradeCalculator;
    private final StudentServicePort studentServicePort;

    public Page<StudentGradeResponse> getStudentGrades(Long examId, Pageable pageable) {
        log.debug("Getting student grades for exam: {}", examId);

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamException(ExamErrorCode.EXAM_NOT_FOUND));

        List<ExamResult> results = resultRepository.findByExamId(examId);
        Map<Long, StudentServicePort.StudentInfo> studentMap = fetchStudentMap(results);

        List<Integer> allScores = results.stream()
                .map(ExamResult::getTotalScore)
                .toList();

        GradeScale gradeScale = exam.getGradeScale() != null ? exam.getGradeScale() : GradeScale.NINE_GRADE;

        List<StudentGradeResponse> grades = results.stream()
                .map(result -> {
                    StudentServicePort.StudentInfo student = resolveStudent(studentMap, result);
                    int rank = statisticsCalculator.calculateRank(result.getTotalScore(), allScores);
                    double percentile = statisticsCalculator.calculatePercentile(result.getTotalScore(), allScores);

                    Integer grade = result.getGrade();
                    if (exam.getCategory() == ExamCategory.GRADED &&
                        exam.getGradingType() == GradingType.RELATIVE) {
                        grade = gradeCalculator.calculateRelativeGrade(result.getTotalScore(), allScores, gradeScale);
                    }

                    return new StudentGradeResponse(
                            result.getStudentId(),
                            student.phone(),
                            result.getStudentId() != null,
                            student.name(),
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
                .sorted(exam.getCategory() == ExamCategory.PASS_FAIL
                        ? Comparator.comparing(StudentGradeResponse::isPassed)
                                .thenComparingInt(StudentGradeResponse::score)
                        : Comparator.comparingInt(StudentGradeResponse::rank))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), grades.size());
        List<StudentGradeResponse> pageContent = grades.subList(start, end);

        return new PageImpl<>(pageContent, pageable, grades.size());
    }

    public List<StudentResultSummaryResponse> getStudentResultSummaries(Long examId) {
        log.debug("Getting student result summaries for exam: {}", examId);

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamException(ExamErrorCode.EXAM_NOT_FOUND));

        List<ExamResult> results = resultRepository.findByExamId(examId);
        Map<Long, StudentServicePort.StudentInfo> studentMap = fetchStudentMap(results);

        return results.stream()
                .map(result -> {
                    StudentServicePort.StudentInfo student = resolveStudent(studentMap, result);
                    return new StudentResultSummaryResponse(
                            result.getId(),
                            result.getStudentId(),
                            result.getPhoneNumber(),
                            result.getStudentId() != null,
                            student.name(),
                            student.academyName(),
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

    private Map<Long, StudentServicePort.StudentInfo> fetchStudentMap(List<ExamResult> results) {
        List<Long> studentIds = results.stream()
                .map(ExamResult::getStudentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (studentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return studentServicePort.getStudentInfoBatch(studentIds);
    }

    private static StudentServicePort.StudentInfo resolveStudent(
            Map<Long, StudentServicePort.StudentInfo> studentMap, ExamResult result) {
        if (result.getStudentId() == null) {
            String displayName = result.getPhoneNumber() != null
                    ? result.getPhoneNumber() : "미등록 학생";
            return new StudentServicePort.StudentInfo(
                    null, displayName, null, result.getPhoneNumber(), "", false);
        }
        return studentMap.getOrDefault(result.getStudentId(),
                new StudentServicePort.StudentInfo(
                        result.getStudentId(), "학생 " + result.getStudentId(),
                        null, null, "", true));
    }
}
