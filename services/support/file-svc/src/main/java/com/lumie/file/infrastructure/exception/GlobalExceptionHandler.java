package com.lumie.file.infrastructure.exception;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
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

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        log.warn("Business exception: {} - {}", errorCode.getCode(), ex.getMessage());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(new ErrorResponse(
                        errorCode.getCode(),
                        ex.getMessage(),
                        LocalDateTime.now()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed: {}", errors);

        return ResponseEntity
                .badRequest()
                .body(new ValidationErrorResponse(
                        "VALIDATION_ERROR",
                        "Validation failed",
                        errors,
                        LocalDateTime.now()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);

        return ResponseEntity
                .internalServerError()
                .body(new ErrorResponse(
                        "INTERNAL_ERROR",
                        "An unexpected error occurred",
                        LocalDateTime.now()
                ));
    }

    public record ErrorResponse(
            String code,
            String message,
            LocalDateTime timestamp
    ) {
    }

    public record ValidationErrorResponse(
            String code,
            String message,
            Map<String, String> errors,
            LocalDateTime timestamp
    ) {
    }
}
