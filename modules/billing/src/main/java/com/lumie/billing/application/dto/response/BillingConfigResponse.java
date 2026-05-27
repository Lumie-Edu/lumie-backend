package com.lumie.billing.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Public billing-widget configuration the browser needs to launch the Toss
 * payment SDK (client key + sandbox flag). Returned by
 * {@code GET /v1/billing/config}.
 *
 * <p>The client key is a Toss "publishable" identifier (per Toss docs it is
 * safe to expose to browsers); the corresponding {@code TOSS_SECRET_KEY} and
 * {@code TOSS_WEBHOOK_SECRET} stay BE-only.
 */
public record BillingConfigResponse(
        @Schema(description = "Toss Payments 브라우저용 클라이언트 키 (test_ck_* 또는 live_ck_*)")
        String tossClientKey,
        @Schema(description = "true면 Toss Sandbox 환경, false면 라이브")
        boolean sandbox
) {}
