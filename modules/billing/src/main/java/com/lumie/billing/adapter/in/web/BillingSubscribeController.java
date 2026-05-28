package com.lumie.billing.adapter.in.web;

import com.lumie.billing.application.dto.request.SubscribeRequest;
import com.lumie.billing.application.dto.response.SubscribeResponse;
import com.lumie.billing.application.service.BillingSubscribeService;
import com.lumie.common.idempotency.IdempotencyService;
import com.lumie.common.tenant.TenantContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Single-shot subscribe-to-paid-plan endpoint. See
 * {@link BillingSubscribeService} for the orchestrated flow rationale.
 *
 * <p>{@code Idempotency-Key} is required (HARD_RULES: POST endpoints creating
 * resources). Duplicate requests within the dedup window return the original
 * {@link SubscribeResponse} body without re-executing the Toss calls.
 */
@RestController
@RequestMapping("/v1/billing/subscribe")
@RequiredArgsConstructor
public class BillingSubscribeController {

    private static final String SCOPE = "POST /v1/billing/subscribe";

    private final BillingSubscribeService billingSubscribeService;
    private final IdempotencyService idempotencyService;

    @PostMapping
    public ResponseEntity<SubscribeResponse> subscribe(
            @Valid @RequestBody SubscribeRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        Long tenantId = TenantContextHolder.getTenantId();
        String tenantSlug = TenantContextHolder.getTenantSlug();
        return idempotencyService.executeOnce(
                idempotencyKey, SCOPE, request,
                () -> ResponseEntity.ok(
                        billingSubscribeService.subscribe(tenantId, tenantSlug, request, idempotencyKey)));
    }
}
