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
    SUBSCRIPTION_PAUSED("BILLING_104", "Subscription is currently paused", 402),

    // Quota errors
    QUOTA_EXCEEDED("BILLING_201", "Quota exceeded", 403),

    // Invoice errors
    INVOICE_NOT_FOUND("BILLING_301", "Invoice not found", 404),
    INVALID_INVOICE_STATE("BILLING_302", "Invalid invoice state for this operation", 400),
    PAYMENT_AMOUNT_MISMATCH("BILLING_303", "Payment amount does not match invoice", 400),

    // Payment errors
    PAYMENT_FAILED("BILLING_401", "Payment failed", 402),
    PAYMENT_RETRY_EXHAUSTED("BILLING_402", "Payment retry limit exhausted — subscription paused", 402),

    // BillingKey errors
    BILLING_KEY_NOT_FOUND("BILLING_501", "Billing key not found", 404),
    BILLING_KEY_REVOKED("BILLING_502", "Billing key has been revoked", 400),
    BILLING_KEY_ALREADY_ACTIVE("BILLING_503", "An active billing key already exists for this tenant", 409),

    // AlimtalkCredit errors
    ALIMTALK_CREDIT_NOT_FOUND("BILLING_601", "Alimtalk credit account not found", 404),
    ALIMTALK_CREDIT_INSUFFICIENT("BILLING_602", "Insufficient alimtalk credit balance", 402),

    // TaxInvoice errors
    TAX_INVOICE_NOT_FOUND("BILLING_701", "Tax invoice not found", 404),
    TAX_INVOICE_ISSUE_FAILED("BILLING_702", "Tax invoice issuance failed at external provider", 502),
    TAX_INVOICE_ALREADY_ISSUED("BILLING_703", "Tax invoice has already been issued", 409),

    // Webhook errors
    TOSS_WEBHOOK_SIGNATURE_INVALID("BILLING_801", "Invalid Toss webhook signature", 401),
    TOSS_API_ERROR("BILLING_802", "Toss Payments API call failed", 502),

    // General errors
    INTERNAL_ERROR("BILLING_999", "Internal server error", 500);

    private final String code;
    private final String message;
    private final int status;
}
