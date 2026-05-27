package com.lumie.billing.application.dto.external.toss;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Toss Payments /v1/billing/authorizations/issue success response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TossBillingKeyResponse(
        String billingKey,
        String customerKey,
        String cardCompany,
        String cardNumber
) {}
