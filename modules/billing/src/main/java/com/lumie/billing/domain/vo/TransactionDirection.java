package com.lumie.billing.domain.vo;

/**
 * Direction/role of a payment_transactions record in the external billing audit log.
 * Matches V28 CHECK constraint: direction in ('REQUEST', 'RESPONSE', 'WEBHOOK').
 */
public enum TransactionDirection {
    /** Outbound provider call — logged before the request is sent. */
    REQUEST,
    /** Provider result — logged after the call returns. */
    RESPONSE,
    /** Inbound payment gateway webhook — logged on receipt. */
    WEBHOOK
}
