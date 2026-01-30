package com.lumie.billing.adapter.in.web;

import com.lumie.billing.application.dto.request.ChangeSubscriptionRequest;
import com.lumie.billing.application.dto.request.CreateSubscriptionRequest;
import com.lumie.billing.application.dto.response.QuotaCheckResponse;
import com.lumie.billing.application.dto.response.SubscriptionResponse;
import com.lumie.billing.application.port.in.*;
import com.lumie.billing.domain.vo.MetricType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final CreateSubscriptionUseCase createSubscriptionUseCase;
    private final GetSubscriptionUseCase getSubscriptionUseCase;
    private final ChangeSubscriptionUseCase changeSubscriptionUseCase;
    private final CancelSubscriptionUseCase cancelSubscriptionUseCase;
    private final CheckQuotaUseCase checkQuotaUseCase;

    @PostMapping
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @Valid @RequestBody CreateSubscriptionRequest request) {
        SubscriptionResponse response = createSubscriptionUseCase.createSubscription(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{tenantSlug}")
    public ResponseEntity<SubscriptionResponse> getSubscription(@PathVariable String tenantSlug) {
        SubscriptionResponse response = getSubscriptionUseCase.getSubscription(tenantSlug);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{tenantSlug}")
    public ResponseEntity<SubscriptionResponse> changePlan(
            @PathVariable String tenantSlug,
            @Valid @RequestBody ChangeSubscriptionRequest request) {
        SubscriptionResponse response = changeSubscriptionUseCase.changePlan(tenantSlug, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{tenantSlug}")
    public ResponseEntity<Void> cancelSubscription(@PathVariable String tenantSlug) {
        cancelSubscriptionUseCase.cancelSubscription(tenantSlug);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{tenantSlug}/quota/{metricType}")
    public ResponseEntity<QuotaCheckResponse> checkQuota(
            @PathVariable String tenantSlug,
            @PathVariable MetricType metricType,
            @RequestParam(required = false) Long currentUsage) {
        QuotaCheckResponse response;
        if (currentUsage != null) {
            response = checkQuotaUseCase.checkQuota(tenantSlug, metricType, currentUsage);
        } else {
            response = checkQuotaUseCase.checkQuota(tenantSlug, metricType);
        }
        return ResponseEntity.ok(response);
    }
}
