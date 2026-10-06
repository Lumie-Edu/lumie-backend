package com.lumie.exam.application.service;

import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExamSummaryStatisticsServiceTest {

    private final ExamSummaryStatisticsService service =
            new ExamSummaryStatisticsService(null, null, null, null, null);

    @Test
    void passFailStatisticsDoNotExposeDormantAcademicSettings() {
        Exam exam = exam(ExamCategory.PASS_FAIL);

        var response = service.buildExamStatistics(exam, List.of());

        assertThat(response.gradingType()).isNull();
        assertThat(response.gradeScale()).isNull();
    }

    @Test
    void gradedStatisticsKeepAcademicSettings() {
        Exam exam = exam(ExamCategory.GRADED);

        var response = service.buildExamStatistics(exam, List.of());

        assertThat(response.gradingType()).isEqualTo(GradingType.ABSOLUTE);
        assertThat(response.gradeScale()).isEqualTo(GradeScale.NINE_GRADE);
    }

    private static Exam exam(ExamCategory category) {
        return Exam.create("시험", category, GradingType.ABSOLUTE, GradeScale.NINE_GRADE,
                1, Map.of("1", "1"), Map.of("1", 10), Map.of("1", "CHOICE"), 5);
    }
}
