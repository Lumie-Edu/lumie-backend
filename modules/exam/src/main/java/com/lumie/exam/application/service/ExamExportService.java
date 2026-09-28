package com.lumie.exam.application.service;

import com.lumie.common.csv.CsvWriter;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.student.api.StudentService;
import com.lumie.exam.application.dto.response.statistics.StudentGradeResponse;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.ExamSource;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamExportService {

    private static final String DELETED_STUDENT_NAME = "삭제된 학생";

    private static final List<String> HEADERS = List.of(
        "학생명", "휴대폰", "시험명", "응시일", "점수", "만점",
        "정답수", "오답수", "등수", "백분위", "등급"
    );
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ExamRepository examRepository;
    private final ExamResultRepository resultRepository;
    private final StatisticsCalculator statisticsCalculator;
    private final GradeCalculator gradeCalculator;
    private final StudentService studentService;
    private final StudentGradeQueryService studentGradeQueryService;

    @Transactional(readOnly = true)
    public void writeTenantWideCsv(OutputStream out) {
        try (CsvWriter csv = new CsvWriter(out)) {
            csv.writeHeader(HEADERS);
            for (Exam exam : examRepository.findBySource(ExamSource.EXAM_MANAGEMENT)) {
                writeTenantWideRows(csv, exam);
                csv.flush();
            }
        }
    }

    @Transactional(readOnly = true)
    public CsvExport prepareCsvForExam(
        Long examId,
        Long classId,
        Boolean hasActiveEnrollment,
        Boolean isActive,
        Sort sort
    ) {
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new BusinessException(ExamErrorCode.EXAM_NOT_FOUND));
        List<ExamResult> results = resultRepository.findByExamId(examId);

        return CsvExport.from(buildRowsForSingleExam(
            exam,
            results,
            classId,
            hasActiveEnrollment,
            isActive,
            normalizeSort(sort)
        ));
    }

    private static Sort normalizeSort(Sort sort) {
        return sort == null ? Sort.unsorted() : sort;
    }

    private void writeTenantWideRows(CsvWriter csv, Exam exam) {
        List<ExamResult> results = resultRepository.findByExamId(exam.getId());
        if (results.isEmpty()) {
            return;
        }

        Map<Long, StudentService.StudentData> studentMap = fetchStudentMap(results);
        List<Integer> allScores = results.stream()
            .map(ExamResult::getTotalScore)
            .toList();
        for (ExamResult result : results) {
            writeCsvRow(csv, toTenantWideRow(exam, result, studentMap, allScores));
        }
    }

    private List<ExamCsvRow> buildRowsForSingleExam(
        Exam exam,
        List<ExamResult> results,
        Long classId,
        Boolean hasActiveEnrollment,
        Boolean isActive,
        Sort sort
    ) {
        if (results.isEmpty()) {
            return List.of();
        }

        Map<Long, ExamResult> resultsById = results.stream()
            .filter(result -> result.getId() != null)
            .collect(Collectors.toMap(ExamResult::getId, result -> result));
        Map<Long, StudentGradeResponse> gradesByResultId = studentGradeQueryService.getStudentGrades(
                exam,
                results,
                classId,
                hasActiveEnrollment,
                isActive,
                Pageable.unpaged(sort)
            )
            .getContent()
            .stream()
            .collect(Collectors.toMap(
                StudentGradeResponse::resultId,
                response -> response,
                (left, right) -> left,
                LinkedHashMap::new
            ));

        int totalPossible = exam.calculateTotalPossibleScore();
        return gradesByResultId.values().stream()
            .map(response -> toFilteredRow(exam, totalPossible, resultsById.get(response.resultId()), response))
            .filter(Objects::nonNull)
            .toList();
    }

    private ExamCsvRow toTenantWideRow(
        Exam exam,
        ExamResult result,
        Map<Long, StudentService.StudentData> studentMap,
        List<Integer> allScores
    ) {
        StudentService.StudentData student = resolveStudent(studentMap, result);
        int rank = statisticsCalculator.calculateRank(result.getTotalScore(), allScores);
        double percentile = statisticsCalculator.calculatePercentile(result.getTotalScore(), allScores);
        Integer grade = calculateGrade(exam, result, allScores);

        return new ExamCsvRow(
            Objects.requireNonNullElse(student.studentName(), ""),
            Objects.requireNonNullElse(
                student.studentPhone() != null ? student.studentPhone() : result.getPhoneNumber(),
                ""
            ),
            exam.getName(),
            formatSubmittedDate(result.getCreatedAt()),
            result.getTotalScore(),
            exam.calculateTotalPossibleScore(),
            result.getCorrectCount(),
            result.getIncorrectCount(),
            rank,
            Math.round(percentile * 10) / 10.0,
            grade
        );
    }

    private Integer calculateGrade(Exam exam, ExamResult result, List<Integer> allScores) {
        GradeScale gradeScale = exam.getGradeScale() != null ? exam.getGradeScale() : GradeScale.NINE_GRADE;
        if (exam.getCategory() == ExamCategory.GRADED && exam.getGradingType() == GradingType.RELATIVE) {
            return gradeCalculator.calculateRelativeGrade(result.getTotalScore(), allScores, gradeScale);
        }
        return result.getGrade();
    }

    private ExamCsvRow toFilteredRow(
        Exam exam,
        int totalPossible,
        ExamResult result,
        StudentGradeResponse response
    ) {
        if (result == null) {
            log.warn("Skipping CSV export row for exam {} because result {} was not found in export map",
                exam.getId(), response.resultId());
            return null;
        }
        return new ExamCsvRow(
            Objects.requireNonNullElse(response.studentName(), ""),
            Objects.requireNonNullElse(
                response.phoneNumber() != null ? response.phoneNumber() : result.getPhoneNumber(),
                ""
            ),
            exam.getName(),
            formatSubmittedDate(response.submittedAt()),
            result.getTotalScore(),
            totalPossible,
            result.getCorrectCount(),
            result.getIncorrectCount(),
            response.rank(),
            response.percentile(),
            response.grade()
        );
    }

    public record CsvExport(List<List<String>> rows) {

        private static CsvExport from(List<ExamCsvRow> sourceRows) {
            return new CsvExport(sourceRows.stream().map(ExamExportService::toCsvColumns).toList());
        }

        public void writeTo(OutputStream out) {
            try (CsvWriter csv = new CsvWriter(out)) {
                csv.writeHeader(HEADERS);
                for (List<String> row : rows) {
                    csv.writeRow(row);
                }
                csv.flush();
            }
        }
    }

    private static void writeCsvRow(CsvWriter csv, ExamCsvRow row) {
        csv.writeRow(toCsvColumns(row));
    }

    private static List<String> toCsvColumns(ExamCsvRow row) {
        return List.of(
            row.studentName(),
            row.phoneNumber(),
            row.examName(),
            row.submittedDate(),
            Integer.toString(row.score()),
            Integer.toString(row.totalPossible()),
            Integer.toString(row.correctCount()),
            Integer.toString(row.incorrectCount()),
            Integer.toString(row.rank()),
            Double.toString(row.percentile()),
            row.grade() == null ? "" : row.grade().toString()
        );
    }

    private static String formatSubmittedDate(Instant submittedAt) {
        return submittedAt == null ? ""
            : LocalDate.ofInstant(submittedAt, ZoneId.systemDefault()).format(DATE_FMT);
    }

    private Map<Long, StudentService.StudentData> fetchStudentMap(List<ExamResult> results) {
        List<Long> studentIds = results.stream()
            .map(ExamResult::getStudentId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (studentIds.isEmpty()) return Collections.emptyMap();
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        return studentService.getStudentsByIds(tenantSlug, studentIds).students().stream()
            .collect(Collectors.toMap(StudentService.StudentData::id, s -> s));
    }

    private static StudentService.StudentData resolveStudent(
        Map<Long, StudentService.StudentData> studentMap, ExamResult result) {
        if (result.getStudentId() == null) {
            if (result.isStudentDeleted()) {
                return new StudentService.StudentData(
                    null, null, DELETED_STUDENT_NAME, null, false, null, null);
            }
            String displayName = result.getPhoneNumber() != null ? result.getPhoneNumber() : "미등록 학생";
            return new StudentService.StudentData(null, null, displayName, result.getPhoneNumber(), false, null, null);
        }
        return studentMap.getOrDefault(result.getStudentId(),
            new StudentService.StudentData(result.getStudentId(), null, DELETED_STUDENT_NAME, null, false, null, null));
    }

    private record ExamCsvRow(
        String studentName,
        String phoneNumber,
        String examName,
        String submittedDate,
        int score,
        int totalPossible,
        int correctCount,
        int incorrectCount,
        int rank,
        double percentile,
        Integer grade
    ) {
    }
}
