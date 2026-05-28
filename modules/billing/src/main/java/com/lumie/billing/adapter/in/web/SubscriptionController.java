package com.lumie.billing.adapter.in.web;

import com.lumie.billing.application.dto.request.ChangeSubscriptionRequest;
import com.lumie.billing.application.dto.request.CreateSubscriptionRequest;
import com.lumie.billing.application.dto.response.InvoiceResponse;
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

import java.util.Map;

@RestController
@RequestMapping("/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private static final String CREATE_SUBSCRIPTION_SCOPE = "POST /v1/subscriptions";
    private static final String CHARGE_FIRST_INVOICE_SCOPE = "POST /v1/subscriptions/{tenantSlug}/charge";
    private static final String CHANGE_PLAN_SCOPE = "PATCH /v1/subscriptions/{tenantSlug}";
    private static final String CANCEL_SUBSCRIPTION_SCOPE = "DELETE /v1/subscriptions/{tenantSlug}";
    private static final String UNDO_CANCELLATION_SCOPE = "DELETE /v1/subscriptions/{tenantSlug}/cancellation";

    private final SubscriptionCommandService subscriptionCommandService;
    private final SubscriptionQueryService subscriptionQueryService;
    private final IdempotencyService idempotencyService;

    @PostMapping
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @Valid @RequestBody CreateSubscriptionRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return idempotencyService.executeOnce(
                idempotencyKey,
                CREATE_SUBSCRIPTION_SCOPE,
                request,
                () -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(subscriptionCommandService.createSubscription(request)));
    }

    @GetMapping("/{tenantSlug}")
    public ResponseEntity<SubscriptionResponse> getSubscription(@PathVariable String tenantSlug) {
        SubscriptionResponse response = subscriptionQueryService.getSubscription(tenantSlug);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{tenantSlug}")
    public ResponseEntity<SubscriptionResponse> changePlan(
            @PathVariable String tenantSlug,
            @Valid @RequestBody ChangeSubscriptionRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return idempotencyService.executeOnce(
                idempotencyKey,
                CHANGE_PLAN_SCOPE,
                Map.of("tenantSlug", tenantSlug, "request", request),
                () -> ResponseEntity.ok(subscriptionCommandService.changePlan(tenantSlug, request)));
    }

    @DeleteMapping("/{tenantSlug}")
    public ResponseEntity<Void> cancelSubscription(
            @PathVariable String tenantSlug,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return idempotencyService.executeOnce(
                idempotencyKey,
                CANCEL_SUBSCRIPTION_SCOPE,
                Map.of("tenantSlug", tenantSlug, "op", "cancel"),
                () -> {
                    subscriptionCommandService.cancelSubscription(tenantSlug);
                    return ResponseEntity.noContent().build();
                });
    }

    @DeleteMapping("/{tenantSlug}/cancellation")
    public ResponseEntity<SubscriptionResponse> undoCancellation(
            @PathVariable String tenantSlug,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return idempotencyService.executeOnce(
                idempotencyKey,
                UNDO_CANCELLATION_SCOPE,
                Map.of("tenantSlug", tenantSlug, "op", "undo_cancellation"),
                () -> ResponseEntity.ok(subscriptionCommandService.undoCancellation(tenantSlug)));
    }

    @PostMapping("/{tenantSlug}/charge")
    public ResponseEntity<InvoiceResponse> chargeFirstInvoice(
            @PathVariable String tenantSlug,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return idempotencyService.executeOnce(
                idempotencyKey,
                CHARGE_FIRST_INVOICE_SCOPE,
                tenantSlug,
                () -> ResponseEntity.ok(subscriptionCommandService.chargeFirstInvoice(tenantSlug)));
    }

    @GetMapping("/{tenantSlug}/quota/{metricType}")
    public ResponseEntity<QuotaCheckResponse> checkQuota(
            @PathVariable String tenantSlug,
            @PathVariable MetricType metricType,
            @RequestParam(defaultValue = "0") long currentUsage) {
        return ResponseEntity.ok(subscriptionQueryService.checkQuota(tenantSlug, metricType, currentUsage));
    }
}
