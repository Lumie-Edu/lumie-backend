package com.lumie.billing.adapter.in.internal;

import com.lumie.billing.application.dto.response.AlimtalkCreditResponse;
import com.lumie.billing.application.dto.response.PlanLimitsResponse;
import com.lumie.billing.application.dto.response.PlanResponse;
import com.lumie.billing.application.dto.response.SubscriptionResponse;
import com.lumie.billing.application.service.AlimtalkCreditCommandService;
import com.lumie.billing.application.service.PlanQueryService;
import com.lumie.billing.application.service.SubscriptionCommandService;
import com.lumie.billing.application.service.SubscriptionQueryService;
import com.lumie.billing.api.BillingService;
import com.lumie.common.quota.MetricType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of the cross-module BillingService contract (libs/internal-api).
 *
 * <p>Exposes billing capabilities to other modules:
 * <ul>
 *   <li>{@link #getActiveSubscription(Long)} — used by any module needing to gate by plan</li>
 *   <li>{@link #consumeAlimtalkCredit(Long, int)} — called by notification module before sending alimtalk</li>
 *   <li>{@link #checkQuota(String, MetricType)} — quota enforcement for student/academy modules</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillingServiceAdapter implements BillingService {

    private final SubscriptionQueryService subscriptionQueryService;
    private final SubscriptionCommandService subscriptionCommandService;
    private final PlanQueryService planQueryService;
    private final AlimtalkCreditCommandService alimtalkCreditCommandService;

    @Override
    public Optional<SubscriptionData> getSubscription(String tenantSlug) {
        try {
            SubscriptionResponse sub = subscriptionQueryService.getSubscription(tenantSlug);
            return Optional.of(toSubscriptionData(sub));
        } catch (Exception e) {
            log.warn("Subscription not found for tenant: {}", tenantSlug);
            return Optional.empty();
        }
    }

    public Optional<SubscriptionData> getActiveSubscription(Long tenantId) {
        try {
            SubscriptionResponse sub = subscriptionQueryService.getSubscriptionByTenantId(tenantId);
            return Optional.of(toSubscriptionData(sub));
        } catch (Exception e) {
            log.warn("Subscription not found for tenantId: {}", tenantId);
            return Optional.empty();
        }
    }

    @Override
    public QuotaResult checkQuota(String tenantSlug, MetricType metricType) {
        // TODO: remove bypass once quota tracking is re-implemented (V31 UsageLog)
        return new QuotaResult(true, 0, Long.MAX_VALUE, metricType,
                "Billing integration pending - unlimited access granted");
    }

    @Override
    public Optional<PlanFeaturesData> getPlanFeatures(String planId) {
        try {
            PlanResponse plan = planQueryService.getPlanById(planId);
            List<String> features = new ArrayList<>();
            if (plan.features().customDomains()) features.add("custom_domains");
            if (plan.features().advancedAnalytics()) features.add("advanced_analytics");
            if (plan.features().prioritySupport()) features.add("priority_support");
            if (plan.features().apiAccess()) features.add("api_access");
            if (plan.features().whiteLabeling()) features.add("white_labeling");
            features.addAll(plan.features().additionalFeatures());
            return Optional.of(new PlanFeaturesData(
                    plan.id(), plan.name(), toPlanLimits(plan.limits()), features));
        } catch (Exception e) {
            log.warn("Plan not found: {}", planId);
            return Optional.empty();
        }
    }

    @Override
    public SubscriptionData createFreeSubscription(Long tenantId, String tenantSlug) {
        SubscriptionResponse sub = subscriptionCommandService.createFreeSubscription(tenantId, tenantSlug);
        return toSubscriptionData(sub);
    }

    /**
     * Consumes alimtalk credits. Called by notification module via libs/internal-api.
     * Throws BillingException with ALIMTALK_CREDIT_INSUFFICIENT if balance is too low.
     * TODO: add consumeAlimtalkCredit to BillingService interface in libs/internal-api when needed.
     */
    public void consumeAlimtalkCredit(Long tenantId, int count) {
        alimtalkCreditCommandService.consume(tenantId, count);
        log.debug("Alimtalk credit consumed via internal-api: tenantId={}, count={}", tenantId, count);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private SubscriptionData toSubscriptionData(SubscriptionResponse sub) {
        return new SubscriptionData(
                sub.id(), sub.tenantId(), sub.tenantSlug(),
                sub.planId(), sub.planName(), sub.status().name(),
                sub.startedAt().toString(),
                sub.expiresAt() != null ? sub.expiresAt().toString() : null,
                toPlanLimits(sub.limits())
        );
    }

    private PlanLimits toPlanLimits(PlanLimitsResponse limits) {
        if (limits == null) return null;
        return new PlanLimits(
                limits.maxStudents(), limits.maxAcademies(),
                limits.maxAdmins(), limits.omrMonthlyQuota()
        );
    }
}
