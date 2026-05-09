package com.lumie.exam.application.service;

import com.lumie.common.csv.CsvWriter;
import com.lumie.exam.application.port.out.StudentServicePort;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
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

import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExamExportService {

    private static final List<String> HEADERS = List.of(
        "학생명", "휴대폰", "시험명", "응시일", "점수", "만점",
        "정답수", "오답수", "등수", "백분위", "등급"
    );
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ExamRepository examRepository;
    private final ExamResultRepository resultRepository;
    private final StatisticsCalculator statisticsCalculator;
    private final GradeCalculator gradeCalculator;
    private final StudentServicePort studentServicePort;

    public void writeCsv(OutputStream out) {
        try (CsvWriter csv = new CsvWriter(out)) {
            csv.writeHeader(HEADERS);

            List<Exam> exams = examRepository.findAll();
            for (Exam exam : exams) {
                writeExamResults(csv, exam);
                csv.flush();
            }
        }
    }

    private void writeExamResults(CsvWriter csv, Exam exam) {
        List<ExamResult> results = resultRepository.findByExamId(exam.getId());
        if (results.isEmpty()) return;

        Map<Long, StudentServicePort.StudentInfo> studentMap = fetchStudentMap(results);

        List<Integer> allScores = results.stream()
            .map(ExamResult::getTotalScore)
            .toList();

        int totalPossible = exam.calculateTotalPossibleScore();
        GradeScale gradeScale = exam.getGradeScale() != null ? exam.getGradeScale() : GradeScale.NINE_GRADE;
        boolean isRelative = exam.getCategory() == ExamCategory.GRADED
            && exam.getGradingType() == GradingType.RELATIVE;

        for (ExamResult result : results) {
            StudentServicePort.StudentInfo student = resolveStudent(studentMap, result);
            int rank = statisticsCalculator.calculateRank(result.getTotalScore(), allScores);
            double percentile = statisticsCalculator.calculatePercentile(result.getTotalScore(), allScores);
            Integer grade = result.getGrade();
            if (isRelative) {
                grade = gradeCalculator.calculateRelativeGrade(result.getTotalScore(), allScores, gradeScale);
            }

            csv.writeRow(List.of(
                Objects.requireNonNullElse(student.name(), ""),
                Objects.requireNonNullElse(student.phone() != null ? student.phone() : result.getPhoneNumber(), ""),
                exam.getName(),
                result.getCreatedAt() == null ? "" : result.getCreatedAt().toLocalDate().format(DATE_FMT),
                Integer.toString(result.getTotalScore()),
                Integer.toString(totalPossible),
                Integer.toString(result.getCorrectCount()),
                Integer.toString(result.getIncorrectCount()),
                Integer.toString(rank),
                Double.toString(Math.round(percentile * 10) / 10.0),
                grade == null ? "" : grade.toString()
            ));
        }
    }

    private Map<Long, StudentServicePort.StudentInfo> fetchStudentMap(List<ExamResult> results) {
        List<Long> studentIds = results.stream()
            .map(ExamResult::getStudentId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (studentIds.isEmpty()) return Collections.emptyMap();
        return studentServicePort.getStudentInfoBatch(studentIds);
    }

    private static StudentServicePort.StudentInfo resolveStudent(
        Map<Long, StudentServicePort.StudentInfo> studentMap, ExamResult result) {
        if (result.getStudentId() == null) {
            String displayName = result.getPhoneNumber() != null ? result.getPhoneNumber() : "미등록 학생";
            return new StudentServicePort.StudentInfo(null, displayName, null, result.getPhoneNumber(), false);
        }
        return studentMap.getOrDefault(result.getStudentId(),
            new StudentServicePort.StudentInfo(result.getStudentId(), "학생 " + result.getStudentId(), null, null, true));
    }
}
