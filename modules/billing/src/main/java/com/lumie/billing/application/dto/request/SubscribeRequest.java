package com.lumie.billing.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Single-shot subscribe-to-paid-plan request.
 *
 * <p>Replaces the FE-coordinated 3-call dance (registerBillingKey + changePlan
 * + chargeFirstInvoice) with one atomic BE operation: issue the billing key
 * from Toss, attach it + change the plan in a single DB tx, then charge.
 *
 * <p>{@code authKey} and {@code customerKey} come from the Toss billing
 * widget's successUrl callback; {@code planId} is the FREE→paid target.
 */
public record SubscribeRequest(
        @Schema(description = "Toss billing-widget authKey (one-shot, from successUrl callback)")
        @NotBlank
        String authKey,

        @Schema(description = "Per-tenant customerKey the FE sent to Toss (e.g. lumie-tenant-15)")
        @NotBlank
        String customerKey,

        @Schema(description = "Target paid plan id (e.g. BASIC, PRO, ENTERPRISE)")
        @NotBlank
        String planId
) {}
