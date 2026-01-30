package com.lumie.billing.infrastructure.exception;

import com.lumie.billing.domain.exception.InvalidSubscriptionStateException;
import com.lumie.billing.domain.exception.PaymentFailedException;
import com.lumie.billing.domain.exception.QuotaExceededException;
import com.lumie.common.exception.DuplicateResourceException;
import com.lumie.common.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(ResourceNotFoundException e) {
        log.warn("Resource not found: {}", e.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, e.getMessage());
        problem.setType(URI.create("https://lumie.com/errors/not-found"));
        problem.setTitle("Resource Not Found");
        problem.setProperty("timestamp", LocalDateTime.now());
        problem.setProperty("code", BillingErrorCode.SUBSCRIPTION_NOT_FOUND.getCode());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateResource(DuplicateResourceException e) {
        log.warn("Duplicate resource: {}", e.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, e.getMessage());
        problem.setType(URI.create("https://lumie.com/errors/conflict"));
        problem.setTitle("Resource Already Exists");
        problem.setProperty("timestamp", LocalDateTime.now());
        problem.setProperty("code", BillingErrorCode.SUBSCRIPTION_ALREADY_EXISTS.getCode());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(InvalidSubscriptionStateException.class)
    public ResponseEntity<ProblemDetail> handleInvalidSubscriptionState(InvalidSubscriptionStateException e) {
        log.warn("Invalid subscription state: {}", e.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, e.getMessage());
        problem.setType(URI.create("https://lumie.com/errors/invalid-state"));
        problem.setTitle("Invalid Subscription State");
        problem.setProperty("timestamp", LocalDateTime.now());
        problem.setProperty("code", BillingErrorCode.INVALID_SUBSCRIPTION_STATE.getCode());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<ProblemDetail> handleQuotaExceeded(QuotaExceededException e) {
        log.warn("Quota exceeded: {}", e.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, e.getMessage());
        problem.setType(URI.create("https://lumie.com/errors/quota-exceeded"));
        problem.setTitle("Quota Exceeded");
        problem.setProperty("timestamp", LocalDateTime.now());
        problem.setProperty("code", BillingErrorCode.QUOTA_EXCEEDED.getCode());
        problem.setProperty("metricType", e.getMetricType().name());
        problem.setProperty("currentUsage", e.getCurrentUsage());
        problem.setProperty("limit", e.getLimit());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ProblemDetail> handlePaymentFailed(PaymentFailedException e) {
        log.error("Payment failed: {}", e.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.PAYMENT_REQUIRED, e.getMessage());
        problem.setType(URI.create("https://lumie.com/errors/payment-failed"));
        problem.setTitle("Payment Failed");
        problem.setProperty("timestamp", LocalDateTime.now());
        problem.setProperty("code", BillingErrorCode.PAYMENT_FAILED.getCode());

        if (e.getPaymentKey() != null) {
            problem.setProperty("paymentKey", e.getPaymentKey());
        }
        if (e.getErrorCode() != null) {
            problem.setProperty("errorCode", e.getErrorCode());
        }

        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("Validation failed: {}", e.getMessage());

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setType(URI.create("https://lumie.com/errors/validation"));
        problem.setTitle("Validation Error");
        problem.setProperty("timestamp", LocalDateTime.now());
        problem.setProperty("errors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception e) {
        log.error("Unexpected error", e);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setType(URI.create("https://lumie.com/errors/internal"));
        problem.setTitle("Internal Server Error");
        problem.setProperty("timestamp", LocalDateTime.now());
        problem.setProperty("code", BillingErrorCode.INTERNAL_ERROR.getCode());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }
}
