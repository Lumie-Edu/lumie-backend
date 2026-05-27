package com.lumie.billing.domain.vo;

public enum SuspensionReason {
    NONE,
    BILLING_KEY_REVOKED,
    PAYMENT_FAILED_3X,
    MANUAL_PAUSE
}
