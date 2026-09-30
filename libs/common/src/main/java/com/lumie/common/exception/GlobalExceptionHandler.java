package com.lumie.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.logstash.logback.argument.StructuredArguments.keyValue;

/**
 * Global REST exception → {@link ProblemDetail} (RFC 7807 / RFC 9457) mapper.
 *
 * <p>Response shape conforms to {@code application/problem+json}. Standard
 * members: {@code type}, {@code title}, {@code status}, {@code detail},
 * {@code instance}. Lumie-specific extensions go in {@code properties}:
 *
 * <ul>
 *   <li>{@code code} — module-scoped {@link ErrorCode#getCode()} (e.g.
 *       {@code BILLING_001}). Stable identifier for client-side error branching.</li>
 *   <li>{@code timestamp} — {@link Instant#now()} at handler entry, ISO-8601.</li>
 *   <li>{@code tenantSlug} — copied from {@link MDC} when set by
 *       {@code RequestContextFilter}.</li>
 *   <li>Tier-1 typed exceptions ({@link QuotaExceededException} et al.)
 *       contribute their typed getters as additional properties so clients
 *       can render upsell CTAs / retry flows without parsing {@code detail}.</li>
 * </ul>
 *
 * <p>Extends {@link ResponseEntityExceptionHandler} to inherit Spring's
 * native {@code ProblemDetail} mapping for framework exceptions
 * ({@code MethodArgumentTypeMismatchException},
 * {@code MissingServletRequestParameterException}, etc.) — only
 * {@code MethodArgumentNotValidException} is overridden to attach
 * structured per-field violations.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String PROBLEM_TYPE_PREFIX = "urn:lumie:error:";

    // ----- BusinessException (all module-defined business failures) ----------

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(BusinessException ex,
                                                                 HttpServletRequest request) {
        logBusinessException(ex, request);
        ProblemDetail body = buildProblemDetail(ex, request);
        if (ex instanceof ExtensionProperties ep) {
            ep.extensionProperties().forEach(body::setProperty);
        }
        return wrapAsProblemJson(body);
    }

    // ----- Validation (MethodArgumentNotValidException) ----------------------

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<Map<String, Object>> violations = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            violations.add(Map.of(
                    "field", fe.getField(),
                    "message", fe.getDefaultMessage() == null ? "" : fe.getDefaultMessage(),
                    "rejectedValue", String.valueOf(fe.getRejectedValue())
            ));
        }
        log.warn("Validation failed: {}", violations);

        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Request validation failed");
        body.setType(URI.create(PROBLEM_TYPE_PREFIX + "validation-error"));
        body.setTitle("Validation error");
        decorate(body, CommonErrorCode.VALIDATION_ERROR.getCode(), request);
        body.setProperty("violations", violations);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .headers(problemJsonHeaders())
                .body(body);
    }

    // ----- Bean Validation on @RequestParam / @PathVariable (@Validated controllers) -----

    /**
     * Handles {@link ConstraintViolationException} thrown by Spring's method-level
     * validation proxy when a {@code @Validated} controller fails a constraint on a
     * {@code @RequestParam}, {@code @PathVariable}, or service method argument.
     *
     * <p>Distinct from {@link MethodArgumentNotValidException} (which covers
     * {@code @Valid @RequestBody} bean validation). Both map to 400 Bad Request.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex,
                                                                   HttpServletRequest request) {
        List<Map<String, Object>> violations = ex.getConstraintViolations().stream()
                .map(cv -> Map.<String, Object>of(
                        "field", cv.getPropertyPath().toString(),
                        "message", cv.getMessage(),
                        "rejectedValue", String.valueOf(cv.getInvalidValue())))
                .toList();
        log.warn("Constraint violation: {}", violations);

        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Request validation failed");
        body.setType(URI.create(PROBLEM_TYPE_PREFIX + "validation-error"));
        body.setTitle("Validation error");
        decorate(body, CommonErrorCode.VALIDATION_ERROR.getCode(), request);
        body.setProperty("violations", violations);
        return wrapAsProblemJson(body);
    }

    // ----- Misc Spring / JDBC fall-through with custom semantics -------------

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalState(IllegalStateException ex,
                                                            HttpServletRequest request) {
        log.warn("Business rule violation (IllegalState): {}", ex.getMessage());
        ProblemDetail body = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        body.setType(URI.create(PROBLEM_TYPE_PREFIX + "business-rule-violation"));
        body.setTitle("Business rule violation");
        decorate(body, CommonErrorCode.BUSINESS_RULE_VIOLATION.getCode(), request);
        return wrapAsProblemJson(body);
    }

    /**
     * Honest mapping for {@link DataIntegrityViolationException}: branch on the
     * underlying JDBC SQLSTATE so the FE sees a status that matches the actual
     * failure class, not a generic 409 that hides NOT NULL / FK / CHECK
     * violations behind "Resource conflict".
     *
     * <p>SQLSTATE classes ("23000" family) are JDBC-standard (ISO/IEC 9075-2);
     * we read them via {@link SQLException#getSQLState()} so no PostgreSQL
     * driver class needs to be on the classpath of {@code libs/common}.
     *
     * <ul>
     *   <li>{@code 23505} unique_violation → 409 RESOURCE_CONFLICT
     *       (truly a duplicate — the only case a user can resolve by retrying
     *       with different input)</li>
     *   <li>{@code 23502} not_null_violation → 500 INTERNAL_ERROR
     *       (server bug — we sent NULL where the schema requires a value)</li>
     *   <li>{@code 23503} foreign_key_violation → 500 INTERNAL_ERROR
     *       (server bug — referenced row missing; or a TOCTOU race)</li>
     *   <li>{@code 23514} check_violation → 422 BUSINESS_RULE_VIOLATION
     *       (the value is syntactically valid but breaks a domain rule the
     *       application should have validated upstream)</li>
     *   <li>anything else → 500 INTERNAL_ERROR (unknown DB failure)</li>
     * </ul>
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(DataIntegrityViolationException ex,
                                                                       HttpServletRequest request) {
        String sqlState = extractSqlState(ex);
        return switch (sqlState) {
            case "23505" -> {
                log.warn("Unique violation: {}", ex.getMessage());
                ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                        "Resource conflict — a duplicate or constraint violation occurred");
                body.setType(URI.create(PROBLEM_TYPE_PREFIX + "resource-conflict"));
                body.setTitle("Resource conflict");
                decorate(body, CommonErrorCode.RESOURCE_CONFLICT.getCode(), request);
                yield wrapAsProblemJson(body);
            }
            case "23514" -> {
                log.warn("Check constraint violation: {}", ex.getMessage());
                ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Value violates a domain rule");
                body.setType(URI.create(PROBLEM_TYPE_PREFIX + "business-rule-violation"));
                body.setTitle("Business rule violation");
                decorate(body, CommonErrorCode.BUSINESS_RULE_VIOLATION.getCode(), request);
                yield wrapAsProblemJson(body);
            }
            // 23502 NOT NULL, 23503 FK, and anything unrecognized are server-side
            // bugs: the app wrote bad data or violated its own invariants. Log
            // at ERROR with the full chain (not WARN) so on-call sees them.
            default -> {
                log.error("Server-side data integrity violation (sqlState={}): {}",
                        sqlState.isEmpty() ? "<unknown>" : sqlState, ex.getMessage(), ex);
                ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Internal server error");
                body.setType(URI.create(PROBLEM_TYPE_PREFIX + "internal-error"));
                body.setTitle("Internal server error");
                decorate(body, CommonErrorCode.INTERNAL_ERROR.getCode(), request);
                yield wrapAsProblemJson(body);
            }
        };
    }

    private static String extractSqlState(DataIntegrityViolationException ex) {
        Throwable cause = ex.getMostSpecificCause();
        return (cause instanceof SQLException sql && sql.getSQLState() != null)
                ? sql.getSQLState()
                : "";
    }

    private void logBusinessException(BusinessException ex, HttpServletRequest request) {
        ErrorCode errorCode = ex.getErrorCode();
        int status = errorCode.getStatus();
        if (status >= 500) {
            log.error("Business exception [{}]: {}",
                    errorCode.getCode(), ex.getMessage(),
                    keyValue("eventType", "error"),
                    keyValue("errorCode", errorCode.getCode()),
                    keyValue("exceptionClass", ex.getClass().getSimpleName()),
                    keyValue("httpStatus", status),
                    keyValue("httpMethod", request.getMethod()),
                    keyValue("rawPath", request.getRequestURI()),
                    keyValue("outcome", "failure"),
                    ex);
        } else if (status == 401 || status == 403) {
            log.warn("Business exception [{}]: {}",
                    errorCode.getCode(), ex.getMessage(),
                    keyValue("eventType", "error"),
                    keyValue("errorCode", errorCode.getCode()),
                    keyValue("exceptionClass", ex.getClass().getSimpleName()),
                    keyValue("httpStatus", status),
                    keyValue("httpMethod", request.getMethod()),
                    keyValue("rawPath", request.getRequestURI()),
                    keyValue("outcome", "failure"));
        } else {
            log.info("Business exception [{}]: {}",
                    errorCode.getCode(), ex.getMessage(),
                    keyValue("eventType", "error"),
                    keyValue("errorCode", errorCode.getCode()),
                    keyValue("exceptionClass", ex.getClass().getSimpleName()),
                    keyValue("httpStatus", status),
                    keyValue("httpMethod", request.getMethod()),
                    keyValue("rawPath", request.getRequestURI()),
                    keyValue("outcome", "failure"));
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex,
                                                               HttpServletRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        body.setType(URI.create(PROBLEM_TYPE_PREFIX + "illegal-argument"));
        body.setTitle("Illegal argument");
        decorate(body, CommonErrorCode.INVALID_INPUT.getCode(), request);
        return wrapAsProblemJson(body);
    }

    /**
     * Optimistic-lock conflict — surfaces concurrent edits on {@code @Version} entities.
     * Distinguished from {@code DUPLICATE_RESOURCE} (also 409) by the dedicated
     * {@code OPTIMISTIC_LOCK_CONFLICT} code so the FE can show a
     * "이 항목은 다른 곳에서 수정되었습니다" toast specifically for stale-edit scenarios.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLock(OptimisticLockingFailureException ex,
                                                              HttpServletRequest request) {
        log.warn("Optimistic lock conflict: {}", ex.getMessage());
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Resource was modified by another request; refresh and retry.");
        body.setType(URI.create(PROBLEM_TYPE_PREFIX + "optimistic-lock-conflict"));
        body.setTitle("Optimistic lock conflict");
        decorate(body, CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT.getCode(), request);
        return wrapAsProblemJson(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error", ex);
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred");
        body.setType(URI.create(PROBLEM_TYPE_PREFIX + "internal-error"));
        body.setTitle("Internal server error");
        decorate(body, CommonErrorCode.INTERNAL_ERROR.getCode(), request);
        return wrapAsProblemJson(body);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers,
                                                          HttpStatusCode status, WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            if (problem.getProperties() == null || !problem.getProperties().containsKey("code")) {
                CommonErrorCode code = status.is5xxServerError()
                        ? CommonErrorCode.INTERNAL_ERROR : CommonErrorCode.INVALID_INPUT;
                decorate(problem, code.getCode(), request);
            }
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.putAll(headers);
            responseHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON);
            return super.createResponseEntity(body, responseHeaders, status, request);
        }
        return super.createResponseEntity(body, headers, status, request);
    }

    // ----- helpers -----------------------------------------------------------

    private ProblemDetail buildProblemDetail(BusinessException ex, HttpServletRequest request) {
        ErrorCode errorCode = ex.getErrorCode();
        ProblemDetail body = ProblemDetail.forStatusAndDetail(
                HttpStatusCode.valueOf(errorCode.getStatus()),
                ex.getMessage());
        body.setType(URI.create(PROBLEM_TYPE_PREFIX + toKebab(errorCode.getCode())));
        body.setTitle(errorCode.getMessage());
        decorate(body, errorCode.getCode(), request);
        return body;
    }

    private static void decorate(ProblemDetail body, String code, Object requestOrWebRequest) {
        body.setProperty("code", code);
        body.setProperty("timestamp", Instant.now().toString());

        String instance = resolveInstance(requestOrWebRequest);
        if (instance != null) {
            body.setInstance(URI.create(instance));
        }

        String tenantSlug = MDC.get("tenantSlug");
        if (tenantSlug != null) {
            body.setProperty("tenantSlug", tenantSlug);
        }
    }

    private static String resolveInstance(Object requestOrWebRequest) {
        if (requestOrWebRequest instanceof HttpServletRequest hsr) {
            return hsr.getRequestURI();
        }
        if (requestOrWebRequest instanceof WebRequest wr) {
            return wr.getDescription(false).replaceFirst("^uri=", "");
        }
        return null;
    }

    private static ResponseEntity<ProblemDetail> wrapAsProblemJson(ProblemDetail body) {
        return ResponseEntity.status(body.getStatus())
                .headers(problemJsonHeaders())
                .body(body);
    }

    private static HttpHeaders problemJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/problem+json");
        return headers;
    }

    private static String toKebab(String code) {
        return code.toLowerCase(java.util.Locale.ROOT).replace('_', '-');
    }
}
