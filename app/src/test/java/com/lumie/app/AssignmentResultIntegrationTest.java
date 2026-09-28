package com.lumie.app;

import com.lumie.assignment.domain.entity.Assignment;
import com.lumie.assignment.domain.entity.AssignmentSubmission;
import com.lumie.assignment.domain.repository.AssignmentRepository;
import com.lumie.assignment.domain.repository.AssignmentSubmissionRepository;
import com.lumie.assignment.domain.vo.AssignmentStatus;
import com.lumie.assignment.domain.vo.SubmissionMode;
import com.lumie.assignment.domain.vo.EvaluationMode;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.application.dto.request.UpdateQuestionAnswerRequest;
import com.lumie.exam.application.service.ResultCommandService;
import com.lumie.exam.application.service.ExamCommandService;
import com.lumie.exam.application.service.ExamSummaryStatisticsService;
import com.lumie.exam.application.service.StudentGradeQueryService;
import com.lumie.exam.application.dto.request.UpdateExamRequest;
import com.lumie.exam.api.ExamService;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.entity.QuestionResult;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.Pageable;
import static org.assertj.core.api.Assertions.*;

@Tag("integration")
class AssignmentResultIntegrationTest extends FullContextTestBase {
    @Autowired TransactionTemplate transactions;
    @Autowired JdbcTemplate jdbc;
    @Autowired ExamRepository exams;
    @Autowired ExamResultRepository results;
    @Autowired AssignmentRepository assignments;
    @Autowired AssignmentSubmissionRepository submissions;
    @Autowired ResultCommandService commands;
    @Autowired ExamCommandService examCommands;
    @Autowired ExamService examApi;
    @Autowired StudentGradeQueryService grades;
    @Autowired ExamSummaryStatisticsService statistics;

    @BeforeAll
    static void migrate() throws Exception {
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("""
                    DO $$ BEGIN
                      IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'lumie_app') THEN
                        CREATE ROLE lumie_app NOSUPERUSER NOBYPASSRLS;
                      END IF;
                      IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'postgres') THEN
                        CREATE ROLE postgres NOSUPERUSER NOBYPASSRLS;
                      END IF;
                    END $$;
                    """);
        }
        var config = Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration/public").baselineOnMigrate(true).baselineVersion("0");
        config.getConfigurationExtension(PostgreSQLConfigurationExtension.class).setTransactionalLock(false);
        config.load().migrate();
    }

    @Test
    void deletionRemovesBothRowsInOneTransaction() {
        withinTenant(() -> {
            var ids = seed();
            commands.deleteResult(ids.exam(), ids.result());
            assertThat(results.existsById(ids.result())).isFalse();
            assertThat(submissions.existsById(ids.submission())).isFalse();
        });
    }

    @Test
    void rollbackRestoresResultAndSubmissionTogether() {
        withinTenant(() -> {
            var ids = seed();
            assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
                commands.deleteResult(ids.exam(), ids.result());
                throw new IllegalStateException("Simulated failure after result deletion");
            })).isInstanceOf(IllegalStateException.class);
            assertThat(results.existsById(ids.result())).isTrue();
            assertThat(submissions.existsById(ids.submission())).isTrue();
        });
    }

    @Test
    void answerCorrectionPersistsBothResultAndSubmission() {
        withinTenant(() -> {
            var ids = seed();
            commands.updateQuestionAnswer(ids.question(), new UpdateQuestionAnswerRequest("2"));
            var submission = submissions.findById(ids.submission()).orElseThrow();
            assertThat(submission.getScore()).isEqualTo(100);
            assertThat(submission.getAnswers()).containsEntry("1", "2");
            assertThat(results.findById(ids.result()).orElseThrow().getTotalScore()).isEqualTo(100);
        });
    }

    private Ids seed() {
        return transactions.execute(status -> {
            Long classId = jdbc.queryForObject(
                    "INSERT INTO classes (name, tenant_id) VALUES ('과제 테스트 반', 1) RETURNING id", Long.class);
            var exam = exams.save(Exam.createForAssignment("과제", ExamCategory.GRADED,
                    GradingType.ABSOLUTE, GradeScale.NINE_GRADE, 1, Map.of("1", "2"),
                    Map.of("1", 100), Map.of("1", "객관식"), null));
            var result = ExamResult.create(exam, null, 0);
            result.addQuestionResult(QuestionResult.create(1, "1", "2", 100));
            result = results.saveAndFlush(result);
            var assignment = assignments.save(Assignment.create(classId, Instant.now().plusSeconds(3600), 100, null,
                    AssignmentStatus.ACTIVE, SubmissionMode.EXAM_MANUAL, EvaluationMode.EXAM_AUTO, exam.getId(),
                    true, true, false, true, false));
            var submission = AssignmentSubmission.create(assignment.getId(), null);
            submission.submitExam(Map.of("1", "1"), result.getId(), 0, false, "9");
            submission = submissions.saveAndFlush(submission);
            return new Ids(exam.getId(), result.getId(), submission.getId(), result.getQuestionResults().get(0).getId());
        });
    }

    @Test
    void gradeSettingsUseTheSameCohortInAssignmentAndDashboard() {
        withinTenant(() -> {
            var ids = seed();
            transactions.executeWithoutResult(status -> {
                var exam = exams.findById(ids.exam()).orElseThrow();
                for (int score = 10; score <= 90; score += 10) {
                    results.save(ExamResult.create(exam, null, score));
                }
            });
            assertGrade(ids, GradingType.RELATIVE, GradeScale.FIVE_GRADE, 5);
            assertGrade(ids, GradingType.RELATIVE, GradeScale.NINE_GRADE, 8);
            assertGrade(ids, GradingType.ABSOLUTE, GradeScale.NINE_GRADE, 9);
        });
    }

    private void assertGrade(Ids ids, GradingType type, GradeScale scale, int expected) {
        examCommands.updateExam(ids.exam(), new UpdateExamRequest(null, null, type, scale,
                null, null, null, null, null));
        var current = examApi.getResultsForAssignment("assignment-test", ids.exam()).get(ids.result());
        assertThat(current.grade()).isEqualTo(expected);
        assertThat(current.passed()).isNull();
        var dashboardGrade = grades.getStudentGrades(ids.exam(), Pageable.unpaged()).stream()
                .filter(row -> row.resultId().equals(ids.result())).findFirst().orElseThrow();
        assertThat(dashboardGrade.grade()).isEqualTo(expected);
        assertThat(statistics.getExamStatistics(ids.exam()).gradeDistribution())
                .anySatisfy(bucket -> assertThat(bucket.grade()).isEqualTo(expected));
    }

    private void withinTenant(Runnable action) {
        TenantContextHolder.withinContext("assignment-test", 1L, action);
    }

    private record Ids(Long exam, Long result, Long submission, Long question) {}
}
