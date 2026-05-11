package com.lumie.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.warn("Business exception: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
            .status(errorCode.getStatus())
            .body(ErrorResponse.of(errorCode.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        List<FieldViolation> violations = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldViolation(fe.getField(), fe.getDefaultMessage(), describe(fe)))
                .toList();
        log.warn("Validation errors: {}", violations);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(
                "VALIDATION_ERROR",
                "Request validation failed",
                Instant.now(),
                violations
            ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException e) {
        log.warn("Illegal state: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of("ILLEGAL_STATE", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Illegal argument: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of("ILLEGAL_ARGUMENT", e.getMessage()));
    }

    /**
     * Optimistic-lock conflict — surfaces concurrent edits on entities now
     * carrying {@code @Version}. Distinguished from {@code DuplicateResource
     * Exception} (also 409) by the {@code OPTIMISTIC_LOCK_CONFLICT} code so
     * the FE can show a "이 항목은 다른 곳에서 수정되었습니다" toast
     * specifically for stale-edit scenarios rather than the generic
     * duplicate message.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(OptimisticLockingFailureException e) {
        log.warn("Optimistic lock conflict: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ErrorResponse.of("OPTIMISTIC_LOCK_CONFLICT",
                    "Resource was modified by another request; refresh and retry."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred"));
    }

    private static String describe(FieldError fe) {
        Object rejected = fe.getRejectedValue();
        return rejected == null ? null : rejected.toString();
    }

    /**
     * @param errors per-field violations on validation failures; {@code null}
     *               for non-validation errors. Kept nullable rather than empty
     *               so consumers can distinguish "no per-field detail" from
     *               "validated cleanly".
     */
    public record ErrorResponse(
            String code,
            String message,
            Instant timestamp,
            List<FieldViolation> errors
    ) {
        public static ErrorResponse of(String code, String message) {
            return new ErrorResponse(code, message, Instant.now(), null);
        }
    }

    public record FieldViolation(String field, String message, String rejectedValue) {}
}
