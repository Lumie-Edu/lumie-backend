package com.lumie.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression suite for {@link GlobalExceptionHandler#handleDataIntegrityViolation}.
 *
 * <p>Before D1, every DataIntegrityViolationException flattened to 409
 * RESOURCE_CONFLICT. The 24-hour billing debug exposed that this hid NOT NULL
 * and FK violations as if they were duplicate-row conflicts. These tests pin
 * the SQL-state-aware mapping so the regression cannot recur silently.
 *
 * <p>SQLSTATE codes are JDBC standard (ISO/IEC 9075-2 "23" class).
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final HttpServletRequest request = new MockHttpServletRequest("POST", "/v1/billing/subscribe");

    @Test
    @DisplayName("unique_violation (23505) → 409 RESOURCE_CONFLICT")
    void uniqueViolation_returns409() {
        var ex = dataIntegrityWithSqlState("23505");

        ResponseEntity<ProblemDetail> response = handler.handleDataIntegrityViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getProperties()).containsEntry("code", "COMMON_008");
    }

    @Test
    @DisplayName("not_null_violation (23502) → 500 INTERNAL_ERROR (server bug, not user conflict)")
    void notNullViolation_returns500() {
        var ex = dataIntegrityWithSqlState("23502");

        ResponseEntity<ProblemDetail> response = handler.handleDataIntegrityViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProperties()).containsEntry("code", "COMMON_005");
    }

    @Test
    @DisplayName("foreign_key_violation (23503) → 500 INTERNAL_ERROR (referenced row missing = server bug)")
    void foreignKeyViolation_returns500() {
        var ex = dataIntegrityWithSqlState("23503");

        ResponseEntity<ProblemDetail> response = handler.handleDataIntegrityViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProperties()).containsEntry("code", "COMMON_005");
    }

    @Test
    @DisplayName("check_violation (23514) → 422 BUSINESS_RULE_VIOLATION")
    void checkViolation_returns422() {
        var ex = dataIntegrityWithSqlState("23514");

        ResponseEntity<ProblemDetail> response = handler.handleDataIntegrityViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProperties()).containsEntry("code", "COMMON_009");
    }

    @Test
    @DisplayName("non-SQL cause (e.g. raw constraint violation) → 500 INTERNAL_ERROR (unknown DB failure)")
    void noSqlStateCause_returns500() {
        var ex = new DataIntegrityViolationException("boom", new RuntimeException("no sqlstate here"));

        ResponseEntity<ProblemDetail> response = handler.handleDataIntegrityViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProperties()).containsEntry("code", "COMMON_005");
    }

    @Test
    @DisplayName("unrecognized SQL state (e.g. 22001 string_data_right_truncation) → 500 INTERNAL_ERROR")
    void unrecognizedSqlState_returns500() {
        var ex = dataIntegrityWithSqlState("22001");

        ResponseEntity<ProblemDetail> response = handler.handleDataIntegrityViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProperties()).containsEntry("code", "COMMON_005");
    }

    private static DataIntegrityViolationException dataIntegrityWithSqlState(String sqlState) {
        // SQLException(reason, sqlState) so handler reads sqlState from getMostSpecificCause()
        return new DataIntegrityViolationException(
                "constraint violation",
                new SQLException("simulated", sqlState));
    }
}
