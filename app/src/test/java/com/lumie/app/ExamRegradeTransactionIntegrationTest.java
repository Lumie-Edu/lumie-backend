package com.lumie.app;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.application.dto.request.ExamAnswerRegradeConfirmRequest;
import com.lumie.exam.application.port.out.ExamPersistencePort;
import com.lumie.exam.application.port.out.ExamResultPersistencePort;
import com.lumie.exam.application.port.out.OmrSubmissionPersistencePort;
import com.lumie.exam.application.service.ExamRegradeService;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.entity.OmrSubmission;
import com.lumie.exam.domain.entity.OmrSubmissionQuestion;
import com.lumie.exam.domain.entity.QuestionResult;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import jakarta.persistence.EntityManager;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;

@Tag("integration")
class ExamRegradeTransactionIntegrationTest extends FullContextTestBase {
    @Autowired TransactionTemplate transactions;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
    @Autowired ExamPersistencePort exams;
    @Autowired ExamResultPersistencePort results;
    @Autowired OmrSubmissionPersistencePort submissions;
    @Autowired ExamRegradeService regrade;

    @BeforeAll
    static void migrate() throws Exception {
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
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
        var config = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration/public")
                .baselineOnMigrate(true).baselineVersion("0");
        config.getConfigurationExtension(PostgreSQLConfigurationExtension.class).setTransactionalLock(false);
        config.load().migrate();
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO tenants (id, slug, name, status, plan_id)
                    VALUES (1, 'regrade-test', 'Regrade Test', 'ACTIVE', 'BASIC')
                    ON CONFLICT (id) DO NOTHING
                    """);
        }
    }

    @Test
    void failedAuditInsertRollsBackExamResultAndOmrChanges() {
        TenantContextHolder.withinContext("regrade-test", 1L, () -> {
            Ids ids = seed(1, 1);
            var command = new ExamAnswerRegradeConfirmRequest("Renamed", Map.of("1", "4"),
                    Map.of("1", 80), Map.of("1", "ESSAY"));

            assertThatThrownBy(() -> regrade.confirm(ids.examId(), command, Long.MAX_VALUE))
                    .isInstanceOf(Exception.class);

            transactions.executeWithoutResult(status -> {
                entityManager.clear();
                Exam exam = exams.findById(ids.examId()).orElseThrow();
                ExamResult result = results.findByExamId(ids.examId()).getFirst();
                OmrSubmission submission = submissions.findSucceededByExamId(ids.examId()).getFirst();
                assertThat(exam.getName()).isEqualTo("Original");
                assertThat(exam.getCorrectAnswers()).containsEntry("1", "2");
                assertThat(exam.getQuestionScores()).containsEntry("1", 100);
                assertThat(exam.getQuestionTypes()).containsEntry("1", "CHOICE");
                assertThat(result.getTotalScore()).isZero();
                assertThat(submission.getTotalScore()).isZero();
                assertThat(submission.getQuestions().getFirst().getQuestionType()).isEqualTo("CHOICE");
                assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM exam_answer_key_revisions WHERE exam_id = ?", Integer.class,
                        ids.examId())).isZero();
            });
        });
    }

    @Test
    void representativeVolumeCompletesWithinSixtySeconds() {
        TenantContextHolder.withinContext("regrade-test", 1L, () -> {
            Ids ids = seed(500, 1_000);
            String longQuestionType = "A".repeat(40);
            var command = new ExamAnswerRegradeConfirmRequest("Original", Map.of("1", "4"),
                    Map.of("1", 80), Map.of("1", longQuestionType));
            long started = System.nanoTime();
            var receipt = regrade.confirm(ids.examId(), command, ids.actorId());
            long elapsedSeconds = java.time.Duration.ofNanos(System.nanoTime() - started).toSeconds();
            assertThat(receipt.regradedResultCount()).isEqualTo(500);
            assertThat(receipt.updatedSubmissionCount()).isEqualTo(1_000);
            assertThat(elapsedSeconds).isLessThan(60);
            transactions.executeWithoutResult(status -> {
                entityManager.clear();
                assertThat(submissions.findSucceededByExamId(ids.examId()).getFirst()
                        .getQuestions().getFirst().getQuestionType()).isEqualTo(longQuestionType);
            });
        });
    }

    @Test
    void nameOnlyChangeCreatesHistoryWithoutRegrading() {
        TenantContextHolder.withinContext("regrade-test", 1L, () -> {
            Ids ids = seed(1, 0);
            var receipt = regrade.confirm(ids.examId(),
                    new ExamAnswerRegradeConfirmRequest("Renamed", Map.of(),
                            Map.of("1", 100), Map.of("1", "CHOICE")), ids.actorId());
            assertThat(receipt.changedAnswers()).isEmpty();
            assertThat(receipt.changedFields()).containsKey("name");
            assertThat(receipt.regradedResultCount()).isZero();
            assertThat(receipt.updatedSubmissionCount()).isZero();
            assertThat(jdbc.queryForObject(
                    "SELECT changed_fields -> 'name' ->> 'after' FROM exam_answer_key_revisions WHERE exam_id = ?",
                    String.class, ids.examId())).isEqualTo("Renamed");
        });
    }

    @Test
    void changedScoreAndAnswerRegradeOfficialAndOmrScoresUsingNewConfiguration() {
        TenantContextHolder.withinContext("regrade-test", 1L, () -> {
            Ids ids = seed(1, 1);
            var receipt = regrade.confirm(ids.examId(),
                    new ExamAnswerRegradeConfirmRequest("Renamed", Map.of("1", "4"),
                            Map.of("1", 80), Map.of("1", "ESSAY")), ids.actorId());

            assertThat(receipt.regradedResultCount()).isEqualTo(1);
            assertThat(receipt.updatedSubmissionCount()).isEqualTo(1);
            transactions.executeWithoutResult(status -> {
                entityManager.clear();
                Exam exam = exams.findById(ids.examId()).orElseThrow();
                ExamResult result = results.findByExamId(ids.examId()).getFirst();
                OmrSubmission submission = submissions.findSucceededByExamId(ids.examId()).getFirst();
                assertThat(exam.getQuestionScores()).containsEntry("1", 80);
                assertThat(result.getTotalScore()).isEqualTo(80);
                assertThat(result.getQuestionResults().getFirst().getEarnedScore()).isEqualTo(80);
                assertThat(submission.getTotalScore()).isEqualTo(80);
                assertThat(submission.getQuestions().getFirst().getEarnedScore()).isEqualTo(80);
                assertThat(submission.getQuestions().getFirst().getQuestionType()).isEqualTo("ESSAY");
            });
        });
    }

    private Ids seed(int resultCount, int submissionCount) {
        return transactions.execute(status -> {
            Exam exam = exams.save(Exam.create("Original", ExamCategory.GRADED,
                    GradingType.ABSOLUTE, GradeScale.NINE_GRADE, 1,
                    Map.of("1", "2"), Map.of("1", 100), Map.of("1", "CHOICE"), null));
            entityManager.flush();
            Long actorId = jdbc.queryForObject("""
                    INSERT INTO users (user_login_id, password_hash, name, role, status, tenant_id)
                    VALUES (?, 'test-hash', 'Regrade staff', 'INSTRUCTOR', 'ACTIVE', 1)
                    RETURNING id
                    """, Long.class, "regrade-" + java.util.UUID.randomUUID());
            Long jobId = jdbc.queryForObject("""
                    INSERT INTO omr_grading_jobs
                      (exam_id, tenant_id, tenant_slug, status, total_images, processed_images,
                       success_count, fail_count, saved_count, image_keys, version)
                    VALUES (?, 1, 'regrade-test', 'COMPLETED', ?, ?, ?, 0, 0, '[]'::jsonb, 0)
                    RETURNING id
                    """, Long.class, exam.getId(), submissionCount, submissionCount, submissionCount);
            List<ExamResult> resultRows = new ArrayList<>(resultCount);
            for (int index = 0; index < resultCount; index++) {
                ExamResult result = ExamResult.create(exam, null, 0);
                result.addQuestionResult(QuestionResult.create(1, "4", "2", 100));
                resultRows.add(result);
            }
            results.saveAll(resultRows);
            List<OmrSubmission> omrRows = new ArrayList<>(submissionCount);
            for (int index = 0; index < submissionCount; index++) {
                OmrSubmission omr = OmrSubmission.pending(exam, jobId, index,
                        "source-" + index, Instant.now());
                omr.complete(null, 0, 9, "image-" + index,
                        List.of(OmrSubmissionQuestion.create(1, "4", "2", 100, 0, "CHOICE")));
                omrRows.add(omr);
            }
            submissions.saveAll(omrRows);
            entityManager.flush();
            return new Ids(exam.getId(), actorId);
        });
    }

    private record Ids(Long examId, Long actorId) {}
}
