package com.lumie.billing.adapter.in.web;

import com.lumie.billing.application.dto.request.ChangeSubscriptionRequest;
import com.lumie.billing.application.dto.request.CreateSubscriptionRequest;
import com.lumie.billing.application.dto.response.QuotaCheckResponse;
import com.lumie.billing.application.dto.response.SubscriptionResponse;
import com.lumie.billing.application.service.SubscriptionCommandService;
import com.lumie.billing.application.service.SubscriptionQueryService;
import com.lumie.common.quota.MetricType;
import com.lumie.common.idempotency.IdempotencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private static final String CREATE_SUBSCRIPTION_SCOPE = "POST /v1/subscriptions";

    private final SubscriptionCommandService subscriptionCommandService;
    private final SubscriptionQueryService subscriptionQueryService;
    private final IdempotencyService idempotencyService;

    @PostMapping
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @Valid @RequestBody CreateSubscriptionRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        if (idempotencyKey != null) {
            return idempotencyService.executeOnce(
                    idempotencyKey,
                    CREATE_SUBSCRIPTION_SCOPE,
                    request,
                    () -> ResponseEntity.status(HttpStatus.CREATED).body(subscriptionCommandService.createSubscription(request))
            );
        }
        SubscriptionResponse response = subscriptionCommandService.createSubscription(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{tenantSlug}")
    public ResponseEntity<SubscriptionResponse> getSubscription(@PathVariable String tenantSlug) {
        SubscriptionResponse response = subscriptionQueryService.getSubscription(tenantSlug);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{tenantSlug}")
    public ResponseEntity<SubscriptionResponse> changePlan(
            @PathVariable String tenantSlug,
            @Valid @RequestBody ChangeSubscriptionRequest request) {
        SubscriptionResponse response = subscriptionCommandService.changePlan(tenantSlug, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{tenantSlug}")
    public ResponseEntity<Void> cancelSubscription(@PathVariable String tenantSlug) {
        subscriptionCommandService.cancelSubscription(tenantSlug);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{tenantSlug}/quota/{metricType}")
    public ResponseEntity<QuotaCheckResponse> checkQuota(
            @PathVariable String tenantSlug,
            @PathVariable MetricType metricType,
            @RequestParam(required = false) Long currentUsage) {
        QuotaCheckResponse response;
        if (currentUsage != null) {
            response = subscriptionQueryService.checkQuota(tenantSlug, metricType, currentUsage);
        } else {
            response = subscriptionQueryService.checkQuota(tenantSlug, metricType);
        }
        return ResponseEntity.ok(response);
    }
}
