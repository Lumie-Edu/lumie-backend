package com.lumie.exam.application.service;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.application.dto.response.ReportBatchDataResponse;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.entity.QuestionResult;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.test.ExamFixtures;
import com.lumie.student.api.StudentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ReportBatchDataQueryServiceTest {

    @Mock
    private ExamRepository examRepository;
    @Mock
    private ExamResultRepository resultRepository;
    @Mock
    private StudentService studentService;

    private ReportBatchDataQueryService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenant("test-academy");
        StatisticsCalculator statisticsCalculator = new StatisticsCalculator();
        GradeCalculator gradeCalculator = new GradeCalculator();
        ExamSummaryStatisticsService examSummaryStatisticsService = new ExamSummaryStatisticsService(
                null, null, statisticsCalculator, gradeCalculator);
        service = new ReportBatchDataQueryService(
                examRepository,
                resultRepository,
                examSummaryStatisticsService,
                studentService,
                statisticsCalculator,
                gradeCalculator
        );
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void getBatchData_preservesRequestOrderAndReturnsPerStudentFailures() {
        Exam exam = ExamFixtures.aGradedExam(2);
        ReflectionTestUtils.setField(exam, "id", 10L);
        ExamResult studentOne = result(exam, 101L, 1L, 20, false);
        ExamResult studentTwo = result(exam, 102L, 2L, 40, true);

        given(examRepository.findById(10L)).willReturn(Optional.of(exam));
        given(resultRepository.findByExamId(10L)).willReturn(List.of(studentOne, studentTwo));
        given(resultRepository.findByStudentIdIn(List.of(2L, 1L, 3L))).willReturn(List.of());
        given(studentService.getStudentsByIds("test-academy", List.of(2L, 1L, 3L)))
                .willReturn(new StudentService.StudentListData(
                        List.of(
                                new StudentService.StudentData(2L, null, "학생2", "010-2", true, null, null),
                                new StudentService.StudentData(1L, null, "학생1", "010-1", true, null, null),
                                new StudentService.StudentData(3L, null, "학생3", "010-3", true, null, null)
                        ),
                        3,
                        0,
                        3
                ));

        ReportBatchDataResponse response = service.getBatchData(10L, List.of(2L, 1L, 3L));

        assertThat(response.examStatistics().participantCount()).isEqualTo(2);
        assertThat(response.items())
                .extracting(ReportBatchDataResponse.Item::studentId)
                .containsExactly(2L, 1L, 3L);
        assertThat(response.items().get(0).success()).isTrue();
        assertThat(response.items().get(0).student().studentName()).isEqualTo("학생2");
        assertThat(response.items().get(1).success()).isTrue();
        assertThat(response.items().get(1).questionResults().get(0).selectedChoice()).isNull();
        assertThat(response.items().get(2).success()).isFalse();
        assertThat(response.items().get(2).error()).isEqualTo("RESULT_NOT_FOUND");
    }

    private static ExamResult result(Exam exam, Long resultId, Long studentId, int score, boolean firstCorrect) {
        ExamResult result = ExamResult.create(exam, studentId, score);
        ReflectionTestUtils.setField(result, "id", resultId);
        result.addQuestionResult(QuestionResult.create(1, firstCorrect ? "1" : null, "1", 20));
        result.addQuestionResult(QuestionResult.create(2, "2", "2", 20));
        return result;
    }
}
