package com.lumie.billing.domain.vo;

/**
 * Direction/role of a payment_transactions record in the PG audit log.
 * Matches V28 CHECK constraint: direction in ('REQUEST', 'RESPONSE', 'WEBHOOK').
 */
public enum TransactionDirection {
    /** Outbound PG call — logged before the HTTP request is sent. */
    REQUEST,
    /** PG call result — logged after the HTTP response is received. */
    RESPONSE,
    /** Inbound webhook from PG — logged on receipt. */
    WEBHOOK
}
