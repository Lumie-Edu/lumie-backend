package com.lumie.billing.adapter.in.internal;

import com.lumie.billing.application.dto.response.PlanResponse;
import com.lumie.billing.application.dto.response.SubscriptionResponse;
import com.lumie.billing.application.service.PlanQueryService;
import com.lumie.billing.application.service.SubscriptionCommandService;
import com.lumie.billing.application.service.SubscriptionQueryService;
import com.lumie.billing.api.BillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingServiceAdapter implements BillingService {

    private final SubscriptionQueryService subscriptionQueryService;
    private final SubscriptionCommandService subscriptionCommandService;
    private final PlanQueryService planQueryService;

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

    @Override
    public QuotaResult checkQuota(String tenantSlug, String metricType) {
        // TODO: TossPayments 연동 전까지 모든 quota 체크 자동 통과
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

    private SubscriptionData toSubscriptionData(SubscriptionResponse sub) {
        return new SubscriptionData(
                sub.id(), sub.tenantId(), sub.tenantSlug(),
                sub.planId(), sub.planName(), sub.status().name(),
                sub.startedAt().toString(),
                sub.expiresAt() != null ? sub.expiresAt().toString() : null,
                toPlanLimits(sub.limits())
        );
    }

    private PlanLimits toPlanLimits(com.lumie.billing.domain.vo.PlanLimits limits) {
        return new PlanLimits(
                limits.maxStudents(), limits.maxAcademies(),
                limits.maxAdmins(), limits.omrMonthlyQuota()
        );
    }
}
