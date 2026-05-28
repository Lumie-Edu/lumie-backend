package com.lumie.billing.application.dto.response;

/**
 * Result of a successful {@code POST /v1/billing/subscribe}:
 * the now-paid subscription, the active billing key, and the first
 * invoice that was charged in the same operation.
 */
public record SubscribeResponse(
        SubscriptionResponse subscription,
        BillingKeyResponse billingKey,
        InvoiceResponse invoice
) {}
