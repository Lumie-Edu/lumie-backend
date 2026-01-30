package com.lumie.billing.infrastructure.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BillingErrorCode {

    // Plan errors
    PLAN_NOT_FOUND("B001", "Plan not found", HttpStatus.NOT_FOUND),

    // Subscription errors
    SUBSCRIPTION_NOT_FOUND("B101", "Subscription not found", HttpStatus.NOT_FOUND),
    SUBSCRIPTION_ALREADY_EXISTS("B102", "Subscription already exists for this tenant", HttpStatus.CONFLICT),
    INVALID_SUBSCRIPTION_STATE("B103", "Invalid subscription state for this operation", HttpStatus.BAD_REQUEST),

    // Quota errors
    QUOTA_EXCEEDED("B201", "Quota exceeded", HttpStatus.FORBIDDEN),

    // Payment errors
    PAYMENT_FAILED("B301", "Payment failed", HttpStatus.PAYMENT_REQUIRED),
    PAYMENT_AMOUNT_MISMATCH("B302", "Payment amount does not match invoice", HttpStatus.BAD_REQUEST),
    INVOICE_NOT_FOUND("B303", "Invoice not found", HttpStatus.NOT_FOUND),
    INVALID_INVOICE_STATE("B304", "Invalid invoice state for this operation", HttpStatus.BAD_REQUEST),

    // General errors
    INTERNAL_ERROR("B999", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
