package com.lumie.exam.adapter.out.persistence;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;
import org.testcontainers.containers.PostgreSQLContainer;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class ExamPersistenceNativeQueriesIntegrationTest {

    private static final Pattern NAMED_PARAMETER = Pattern.compile(":([A-Za-z][A-Za-z0-9_]*)");

    @Test
    void omrAndReportCallbackUpdatesKeepTheirConditionalAndIdempotentBehavior() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")) {
            postgres.start();
            try (Connection connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
                createTables(connection);
                execute(connection, """
                        insert into omr_grading_jobs
                            (id, exam_id, processed_images, success_count, fail_count, status, created_at)
                        values (1, 10, 0, 0, 0, 'PENDING', now()),
                               (2, 10, 0, 0, 0, 'PENDING', now() - interval '2 days')
                        """);
                execute(connection, """
                        insert into report_generation_jobs
                            (id, exam_id, processed_reports, success_count, fail_count, status,
                             zip_file_key, zip_expires_at, created_at)
                        values (1, 10, 0, 0, 0, 'PENDING', null, null, now()),
                               (2, 10, 0, 0, 0, 'PENDING', null, null, now() - interval '2 days')
                        """);

                assertThat(update(connection, OmrGradingJobJpaRepository.class,
                        "markProcessingIfPending", new Class<?>[] {Long.class}, Map.of("jobId", 1L))).isEqualTo(1);
                assertThat(update(connection, OmrGradingJobJpaRepository.class,
                        "markProcessingIfPending", new Class<?>[] {Long.class}, Map.of("jobId", 1L))).isZero();
                assertThat(update(connection, OmrGradingJobJpaRepository.class,
                        "incrementSuccess", new Class<?>[] {Long.class}, Map.of("jobId", 1L))).isEqualTo(1);
                execute(connection, "update omr_grading_jobs set processed_images = 1 where id = 1");
                assertThat(update(connection, OmrGradingJobJpaRepository.class,
                        "markCompletedIfAllProcessed", new Class<?>[] {Long.class, int.class},
                        Map.of("jobId", 1L, "totalImages", 1))).isEqualTo(1);
                assertThat(update(connection, OmrGradingJobJpaRepository.class,
                        "markCompletedIfAllProcessed", new Class<?>[] {Long.class, int.class},
                        Map.of("jobId", 1L, "totalImages", 1))).isZero();
                int zipKeyWrite = update(connection, ReportGenerationJobJpaRepository.class,
                        "setZipFileKey", new Class<?>[] {Long.class, String.class, java.time.Instant.class},
                        Map.of("jobId", 1L, "zipFileKey", "zip/key", "zipExpiresAt",
                                java.sql.Timestamp.from(java.time.Instant.parse("2026-11-02T00:00:00Z"))));
                int duplicateZipKeyWrite = update(connection, ReportGenerationJobJpaRepository.class,
                        "setZipFileKey", new Class<?>[] {Long.class, String.class, java.time.Instant.class},
                        Map.of("jobId", 1L, "zipFileKey", "replacement", "zipExpiresAt",
                                java.sql.Timestamp.from(java.time.Instant.parse("2026-11-03T00:00:00Z"))));
                assertThat(zipKeyWrite).isEqualTo(1);
                assertThat(duplicateZipKeyWrite).isZero();
                assertThat(update(connection, ReportGenerationJobJpaRepository.class,
                        "failStaleJob", new Class<?>[] {Long.class, int.class},
                        Map.of("jobId", 2L, "failedItems", 2))).isEqualTo(1);

                assertThat(scalar(connection, "select status from omr_grading_jobs where id = 1"))
                        .isEqualTo("COMPLETED");
                assertThat(scalar(connection, "select success_count from omr_grading_jobs where id = 1"))
                        .isEqualTo(1);
                assertThat(scalar(connection, "select status from report_generation_jobs where id = 2"))
                        .isEqualTo("FAILED");
                assertThat(scalar(connection, "select processed_reports from report_generation_jobs where id = 2"))
                        .isEqualTo(2);
                assertThat(update(connection, OmrGradingJobJpaRepository.class,
                        "deleteTerminalJobsByExamId", new Class<?>[] {Long.class}, Map.of("examId", 10L)))
                        .isEqualTo(1);
            }
        }
    }

    private static void createTables(Connection connection) throws Exception {
        execute(connection, """
                create table omr_grading_jobs (
                    id bigint primary key, exam_id bigint not null, processed_images integer not null,
                    success_count integer not null, fail_count integer not null, status varchar(20) not null,
                    updated_at timestamptz, created_at timestamptz not null)
                """);
        execute(connection, """
                create table report_generation_jobs (
                    id bigint primary key, exam_id bigint not null, processed_reports integer not null,
                    success_count integer not null, fail_count integer not null, status varchar(20) not null,
                    zip_file_key text, zip_expires_at timestamptz, updated_at timestamptz, created_at timestamptz)
                """);
    }

    private static int update(Connection connection, Class<?> repository, String methodName,
            Class<?>[] parameterTypes, Map<String, ?> values) throws Exception {
        Method method = repository.getMethod(methodName, parameterTypes);
        String sql = method.getAnnotation(Query.class).value();
        Matcher matcher = NAMED_PARAMETER.matcher(sql);
        List<Object> parameters = new ArrayList<>();
        StringBuffer preparedSql = new StringBuffer();
        while (matcher.find()) {
            parameters.add(values.get(matcher.group(1)));
            matcher.appendReplacement(preparedSql, "?");
        }
        matcher.appendTail(preparedSql);
        try (PreparedStatement statement = connection.prepareStatement(preparedSql.toString())) {
            for (int index = 0; index < parameters.size(); index++) {
                statement.setObject(index + 1, parameters.get(index));
            }
            return statement.executeUpdate();
        }
    }

    private static void execute(Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static Object scalar(Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
            rows.next();
            return rows.getObject(1);
        }
    }
}
