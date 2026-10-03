package com.lumie.exam.domain.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExamGradeBucketSqlTest {

    @Test
    void aggregateUsesCurrentRawScoresAndFullRelativeCohort() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")) {
            postgres.start();
            try (var connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 var statement = connection.createStatement()) {
                statement.execute("""
                        create table exams (
                            id bigint primary key, category varchar(20), source varchar(30),
                            grading_type varchar(20), grade_scale varchar(20))
                        """);
                statement.execute("""
                        create table exam_results (
                            exam_id bigint, total_score integer, grade integer)
                        """);
                statement.execute("""
                        insert into exams values
                          (1, 'GRADED', 'EXAM_MANAGEMENT', 'ABSOLUTE', 'NINE_GRADE'),
                          (2, 'GRADED', 'EXAM_MANAGEMENT', 'RELATIVE', 'FIVE_GRADE'),
                          (3, 'PASS_FAIL', 'EXAM_MANAGEMENT', 'ABSOLUTE', null)
                        """);
                statement.execute("""
                        insert into exam_results values
                          (1, 10, 1), (1, 95, 9),
                          (2, 100, null), (2, 90, null), (2, 90, null), (2, 50, null),
                          (3, 10, -1), (3, 0, 0)
                        """);

                String sql = ExamResultRepository.class.getMethod("aggregateGradeBuckets")
                        .getAnnotation(Query.class).value();
                Map<String, Integer> buckets = new HashMap<>();
                try (var rows = statement.executeQuery(sql)) {
                    while (rows.next()) {
                        buckets.put(rows.getString("gradeScale") + ":" + rows.getInt("grade"), rows.getInt("cnt"));
                    }
                }

                assertThat(buckets).containsExactlyInAnyOrderEntriesOf(Map.of(
                        "NINE_GRADE:1", 1, "NINE_GRADE:9", 1,
                        "FIVE_GRADE:1", 1, "FIVE_GRADE:2", 2, "FIVE_GRADE:4", 1));
            }
        }
    }
}
