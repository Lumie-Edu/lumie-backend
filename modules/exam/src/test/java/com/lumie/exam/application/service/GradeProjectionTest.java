package com.lumie.exam.application.service;

import com.lumie.exam.application.dto.response.statistics.ExamStatisticsResponse;
import com.lumie.exam.application.dto.response.statistics.StabilityIndexResponse;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import com.lumie.exam.test.ExamFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class GradeProjectionTest {

    private final GradeCalculator gradeCalculator = new GradeCalculator();
    private final StatisticsCalculator statisticsCalculator = new StatisticsCalculator();

    @Test
    void examDistributionUsesStoredAbsoluteGradeEvenWhenCurrentScoreSuggestsAnotherGrade() {
        Exam exam = ExamFixtures.aGradedExam();
        ExamResult result = ExamFixtures.anExamResult(exam, 1L, 60);
        result.assignGrade(7);
        ExamSummaryStatisticsService service = new ExamSummaryStatisticsService(
                null, null, statisticsCalculator, gradeCalculator, null);

        ExamStatisticsResponse response = service.buildExamStatistics(exam, List.of(result));

        assertThat(response.gradeDistribution()).extracting(ExamStatisticsResponse.GradeDistribution::grade)
                .containsExactly(7);
    }

    @Test
    void relativeDistributionUsesFullExamCohort() {
        Exam exam = Exam.create("상대 시험", ExamCategory.GRADED, GradingType.RELATIVE,
                GradeScale.NINE_GRADE, 1, Map.of("1", "1"), Map.of("1", 100), Map.of("1", "단답"), null);
        ExamResult highest = ExamFixtures.anExamResult(exam, 1L, 100, List.of(true));
        ExamResult lowest = ExamFixtures.anExamResult(exam, 2L, 60, List.of(false));
        ExamRepository examRepository = mock(ExamRepository.class);
        ExamResultRepository resultRepository = mock(ExamResultRepository.class);
        ExamResultCohortResolver cohortResolver = mock(ExamResultCohortResolver.class);
        given(examRepository.findById(1L)).willReturn(java.util.Optional.of(exam));
        given(resultRepository.findByExamId(1L)).willReturn(List.of(highest, lowest));
        given(cohortResolver.filter(List.of(highest, lowest), 10L, null, null)).willReturn(List.of(lowest));
        ExamSummaryStatisticsService service = new ExamSummaryStatisticsService(
                examRepository, resultRepository, statisticsCalculator, gradeCalculator, cohortResolver);

        ExamStatisticsResponse response = service.getExamStatistics(1L, 10L, null, null);
        assertThat(response.gradeDistribution()).extracting(ExamStatisticsResponse.GradeDistribution::grade)
                .containsExactly(5);
    }

    @Test
    void stabilityHistoryLeavesUnknownGradeNullInsteadOfZero() {
        Exam exam = ExamFixtures.aGradedExam();
        ReflectionTestUtils.setField(exam, "id", 1L);
        ExamResult result = ExamFixtures.anExamResult(exam, 1L, 60);
        ReflectionTestUtils.setField(result, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));

        StabilityIndexResponse response = ExamStatisticsProjection.calculateStabilityIndex(
                1L, List.of(result), statisticsCalculator, gradeCalculator, Map.of());

        assertThat(response.scoreHistory()).singleElement().extracting(StabilityIndexResponse.ScoreHistory::grade)
                .isNull();
    }

    @Test
    void stabilityHistoryUsesFullRelativeExamCohortInsteadOfStaleStoredGrade() {
        Exam exam = Exam.create("상대 시험", ExamCategory.GRADED, GradingType.RELATIVE,
                GradeScale.NINE_GRADE, 1, Map.of("1", "1"), Map.of("1", 100), Map.of("1", "단답"), null);
        ReflectionTestUtils.setField(exam, "id", 2L);
        ExamResult result = ExamFixtures.anExamResult(exam, 1L, 60);
        result.assignGrade(8);
        ReflectionTestUtils.setField(result, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));

        StabilityIndexResponse response = ExamStatisticsProjection.calculateStabilityIndex(
                1L, List.of(result), statisticsCalculator, gradeCalculator, Map.of(2L, List.of(100, 60)));

        assertThat(response.scoreHistory()).singleElement()
                .extracting(StabilityIndexResponse.ScoreHistory::grade).isEqualTo(5);
    }

    @Test
    void stabilityEndpointLoadsAllRelativeExamScores() {
        Exam exam = Exam.create("상대 시험", ExamCategory.GRADED, GradingType.RELATIVE,
                GradeScale.NINE_GRADE, 1, Map.of("1", "1"), Map.of("1", 100), Map.of("1", "단답"), null);
        ReflectionTestUtils.setField(exam, "id", 2L);
        ExamResult studentResult = ExamFixtures.anExamResult(exam, 1L, 60);
        ReflectionTestUtils.setField(studentResult, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));
        ExamResult peerResult = ExamFixtures.anExamResult(exam, 2L, 100);
        ExamResultRepository resultRepository = mock(ExamResultRepository.class);
        given(resultRepository.findByStudentId(1L)).willReturn(List.of(studentResult));
        given(resultRepository.findByExamIdIn(Set.of(2L))).willReturn(List.of(studentResult, peerResult));
        StudentAnalyticsQueryService service = new StudentAnalyticsQueryService(
                resultRepository, statisticsCalculator, gradeCalculator);

        StabilityIndexResponse response = service.getStabilityIndex(1L);

        assertThat(response.scoreHistory()).singleElement()
                .extracting(StabilityIndexResponse.ScoreHistory::grade).isEqualTo(5);
    }

    @Test
    void stabilityHistoryKeepsStoredAbsoluteGrade() {
        Exam exam = ExamFixtures.aGradedExam();
        ReflectionTestUtils.setField(exam, "id", 3L);
        ExamResult result = ExamFixtures.anExamResult(exam, 1L, 60);
        result.assignGrade(7);
        ReflectionTestUtils.setField(result, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));

        StabilityIndexResponse response = ExamStatisticsProjection.calculateStabilityIndex(
                1L, List.of(result), statisticsCalculator, gradeCalculator, Map.of());

        assertThat(response.scoreHistory()).singleElement()
                .extracting(StabilityIndexResponse.ScoreHistory::grade).isEqualTo(7);
    }

    @Test
    void passFailProjectsPassStatusWithoutLegacyNumericGrade() {
        Exam exam = Exam.create("단어 시험", ExamCategory.PASS_FAIL, GradingType.ABSOLUTE,
                GradeScale.NINE_GRADE, 1, Map.of("1", "1"), Map.of("1", 20), Map.of("1", "어휘"), 15);
        ReflectionTestUtils.setField(exam, "id", 81L);
        ExamResult result = ExamResult.create(exam, 7L, 20);
        result.assignGrade(8);
        ReflectionTestUtils.setField(result, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));

        assertThat(gradeCalculator.resolveStoredOrRelativeGrade(exam, result, List.of(20))).isNull();
        var response = com.lumie.exam.application.dto.response.ExamResultResponse.from(result);
        assertThat(response.grade()).isNull();
        assertThat(response.isPassed()).isTrue();

        var history = ExamStatisticsProjection.calculateStabilityIndex(
                7L, List.of(result), statisticsCalculator, gradeCalculator, Map.of())
                .scoreHistory().getFirst();
        assertThat(history.grade()).isNull();
        assertThat(history.isPassed()).isTrue();
        assertThat(history.examCategory()).isEqualTo(ExamCategory.PASS_FAIL);
    }
}
