package com.lumie.ai.application.service;

import com.lumie.ai.domain.exception.AiErrorCode;
import com.lumie.ai.domain.exception.AiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SqlValidatorTest {

    private final SqlValidator validator = new SqlValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT 1",
            "  select id from students  ",
            "SELECT 1;;;",
            "WITH current_rows AS (SELECT id FROM students) SELECT id FROM current_rows"
    })
    void acceptsSingleReadOnlyQuery(String sql) {
        assertThatCode(() -> validator.validate(sql)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void rejectsMissingQuery(String sql) {
        assertInvalid(sql, "cannot be empty");
    }

    @Test
    void rejectsMultipleStatementsEvenWhenTheyStartWithSelect() {
        assertInvalid("SELECT 1; SELECT 2", "Multiple statements");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "DELETE FROM students",
            "UPDATE students SET name = 'changed'",
            "VALUES (1)",
            "EXPLAIN SELECT 1"
    })
    void rejectsNonSelectStatement(String sql) {
        assertInvalid(sql, "Only SELECT");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT 1 FROM students FOR UPDATE",
            "SELECT 1 INTO temp_rows",
            "WITH changed AS (DELETE FROM students RETURNING id) SELECT id FROM changed",
            "SELECT 1 FROM students; DROP TABLE students"
    })
    void rejectsMutatingKeywordOrSecondStatement(String sql) {
        assertThatThrownBy(() -> validator.validate(sql))
                .isInstanceOfSatisfying(AiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AiErrorCode.INVALID_SQL));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT * FROM pg_class",
            "SELECT * FROM information_schema.tables"
    })
    void rejectsSystemCatalogAccess(String sql) {
        assertThatThrownBy(() -> validator.validate(sql))
                .isInstanceOfSatisfying(AiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AiErrorCode.INVALID_SQL));
    }

    private void assertInvalid(String sql, String message) {
        assertThatThrownBy(() -> validator.validate(sql))
                .isInstanceOfSatisfying(AiException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(AiErrorCode.INVALID_SQL);
                    assertThat(exception.getMessage()).contains(message);
                });
    }
}
