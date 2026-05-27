package com.lumie.billing.application.service;

import com.lumie.billing.application.dto.response.QuotaCheckResponse;
import com.lumie.billing.application.dto.response.SubscriptionResponse;
import com.lumie.billing.application.port.out.SubscriptionPersistencePort;
import com.lumie.billing.domain.entity.Subscription;
import com.lumie.common.quota.MetricType;
import com.lumie.billing.domain.vo.PlanLimits;
import com.lumie.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionQueryService {

    private final SubscriptionPersistencePort subscriptionPersistencePort;

    public SubscriptionResponse getSubscription(String tenantSlug) {
        log.debug("Fetching subscription for tenant: {}", tenantSlug);
        return subscriptionPersistencePort.findByTenantSlug(tenantSlug)
                .map(SubscriptionResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", tenantSlug));
    }

    public SubscriptionResponse getSubscriptionByTenantId(Long tenantId) {
        log.debug("Fetching subscription for tenantId: {}", tenantId);
        return subscriptionPersistencePort.findByTenantId(tenantId)
                .map(SubscriptionResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "tenantId:" + tenantId));
    }

    public QuotaCheckResponse checkQuota(String tenantSlug, MetricType metricType, long currentUsage) {
        log.debug("Checking quota for tenant: {}, metricType: {}, currentUsage: {}",
                tenantSlug, metricType, currentUsage);

        Subscription subscription = subscriptionPersistencePort.findActiveByTenantSlug(tenantSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", tenantSlug));

        PlanLimits limits = subscription.getLimits();
        int limit = limits.getLimit(metricType);
        boolean allowed = limits.isWithinLimit(metricType, currentUsage);

        if (!allowed) {
            log.warn("Quota exceeded for tenant: {}, metricType: {}, usage: {}/{}",
                    subscription.getTenantSlug(), metricType, currentUsage, limit);
            return QuotaCheckResponse.exceeded(metricType, currentUsage, limit);
        }

        return QuotaCheckResponse.allowed(metricType, currentUsage, limit);
    }
}
