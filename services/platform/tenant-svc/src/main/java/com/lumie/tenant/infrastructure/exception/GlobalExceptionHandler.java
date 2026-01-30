package com.lumie.tenant.infrastructure.exception;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.DuplicateResourceException;
import com.lumie.common.exception.ResourceNotFoundException;
import com.lumie.tenant.domain.exception.InvalidTenantStateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException e) {
        log.warn("Resource not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(e.getErrorCode().getCode(), e.getMessage()));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(DuplicateResourceException e) {
        log.warn("Duplicate resource: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(e.getErrorCode().getCode(), e.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.warn("Business exception: {}", e.getMessage());
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(ErrorResponse.of(e.getErrorCode().getCode(), e.getMessage()));
    }

    @ExceptionHandler(InvalidTenantStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTenantState(InvalidTenantStateException e) {
        log.warn("Invalid tenant state transition: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("T002", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Invalid argument: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("C001", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("C001", "Validation failed", errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Unexpected error occurred", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("C005", "Internal server error"));
    }

    public record ErrorResponse(
            String code,
            String message,
            Map<String, String> errors,
            LocalDateTime timestamp
    ) {
        public static ErrorResponse of(String code, String message) {
            return new ErrorResponse(code, message, null, LocalDateTime.now());
        }

        public static ErrorResponse of(String code, String message, Map<String, String> errors) {
            return new ErrorResponse(code, message, errors, LocalDateTime.now());
        }
    }
}
