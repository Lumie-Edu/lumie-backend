package com.lumie.billing.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BillingErrorCode implements ErrorCode {

    // Plan errors
    PLAN_NOT_FOUND("BILLING_001", "Plan not found", 404),

    // Subscription errors
    SUBSCRIPTION_NOT_FOUND("BILLING_101", "Subscription not found", 404),
    SUBSCRIPTION_ALREADY_EXISTS("BILLING_102", "Subscription already exists for this tenant", 409),
    INVALID_SUBSCRIPTION_STATE("BILLING_103", "Invalid subscription state for this operation", 400),

    // Quota errors
    QUOTA_EXCEEDED("BILLING_201", "Quota exceeded", 403),

    // Payment errors
    PAYMENT_FAILED("BILLING_301", "Payment failed", 402),
    PAYMENT_AMOUNT_MISMATCH("BILLING_302", "Payment amount does not match invoice", 400),
    INVOICE_NOT_FOUND("BILLING_303", "Invoice not found", 404),
    INVALID_INVOICE_STATE("BILLING_304", "Invalid invoice state for this operation", 400),

    // Paddle errors
    PADDLE_WEBHOOK_SIGNATURE_INVALID("BILLING_401", "Invalid Paddle webhook signature", 401),
    PADDLE_API_ERROR("BILLING_402", "Paddle API call failed", 502),

    // General errors
    INTERNAL_ERROR("BILLING_999", "Internal server error", 500);

    private final String code;
    private final String message;
    private final int status;
}
