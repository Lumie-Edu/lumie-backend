package com.lumie.billing.domain.vo;

/** Describes the nature of the PG call recorded in a PaymentTransaction. */
public enum TransactionType {
    SUBSCRIPTION_CHARGE,
    SUBSCRIPTION_REFUND,
    ALIMTALK_RECHARGE,
    BILLING_KEY_ISSUE,
    BILLING_KEY_REVOKE
}
