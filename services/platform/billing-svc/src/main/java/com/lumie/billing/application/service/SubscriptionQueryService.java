package com.lumie.billing.application.service;

import com.lumie.billing.application.dto.response.QuotaCheckResponse;
import com.lumie.billing.application.dto.response.SubscriptionResponse;
import com.lumie.billing.application.port.in.CheckQuotaUseCase;
import com.lumie.billing.application.port.in.GetSubscriptionUseCase;
import com.lumie.billing.application.port.out.BillingEventPublisherPort;
import com.lumie.billing.application.port.out.SubscriptionPersistencePort;
import com.lumie.billing.application.port.out.UsageLogPersistencePort;
import com.lumie.billing.domain.entity.Subscription;
import com.lumie.billing.domain.vo.MetricType;
import com.lumie.billing.domain.vo.PlanLimits;
import com.lumie.common.exception.ResourceNotFoundException;
import com.lumie.messaging.event.QuotaExceededEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionQueryService implements GetSubscriptionUseCase, CheckQuotaUseCase {

    private final SubscriptionPersistencePort subscriptionPersistencePort;
    private final UsageLogPersistencePort usageLogPersistencePort;
    private final BillingEventPublisherPort eventPublisherPort;

    @Override
    public SubscriptionResponse getSubscription(String tenantSlug) {
        log.debug("Fetching subscription for tenant: {}", tenantSlug);
        return subscriptionPersistencePort.findByTenantSlug(tenantSlug)
                .map(SubscriptionResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", tenantSlug));
    }

    @Override
    public SubscriptionResponse getSubscriptionByTenantId(Long tenantId) {
        log.debug("Fetching subscription for tenantId: {}", tenantId);
        return subscriptionPersistencePort.findByTenantId(tenantId)
                .map(SubscriptionResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "tenantId:" + tenantId));
    }

    @Override
    public QuotaCheckResponse checkQuota(String tenantSlug, MetricType metricType) {
        log.debug("Checking quota for tenant: {}, metricType: {}", tenantSlug, metricType);

        Subscription subscription = subscriptionPersistencePort.findActiveByTenantSlug(tenantSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", tenantSlug));

        long currentUsage = getCurrentUsage(subscription.getTenantId(), metricType);
        return checkQuotaInternal(subscription, metricType, currentUsage);
    }

    @Override
    public QuotaCheckResponse checkQuota(String tenantSlug, MetricType metricType, long currentUsage) {
        log.debug("Checking quota for tenant: {}, metricType: {}, currentUsage: {}",
                tenantSlug, metricType, currentUsage);

        Subscription subscription = subscriptionPersistencePort.findActiveByTenantSlug(tenantSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", tenantSlug));

        return checkQuotaInternal(subscription, metricType, currentUsage);
    }

    private QuotaCheckResponse checkQuotaInternal(Subscription subscription, MetricType metricType, long currentUsage) {
        PlanLimits limits = subscription.getLimits();
        int limit = limits.getLimit(metricType);
        boolean allowed = limits.isWithinLimit(metricType, currentUsage);

        if (!allowed) {
            log.warn("Quota exceeded for tenant: {}, metricType: {}, usage: {}/{}",
                    subscription.getTenantSlug(), metricType, currentUsage, limit);

            QuotaExceededEvent event = new QuotaExceededEvent(
                    subscription.getTenantId(),
                    subscription.getTenantSlug(),
                    metricType.name(),
                    currentUsage,
                    limit
            );
            eventPublisherPort.publish(event);

            return QuotaCheckResponse.exceeded(metricType, currentUsage, limit);
        }

        return QuotaCheckResponse.allowed(metricType, currentUsage, limit);
    }

    private long getCurrentUsage(Long tenantId, MetricType metricType) {
        if (metricType == MetricType.OMR_MONTHLY) {
            LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
            LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);
            return usageLogPersistencePort
                    .sumValueByTenantIdAndMetricTypeAndRecordedDateBetween(
                            tenantId, metricType, startOfMonth, endOfMonth)
                    .orElse(0L);
        }

        return usageLogPersistencePort.findLatestByTenantIdAndMetricType(tenantId, metricType)
                .map(usageLog -> usageLog.getValue())
                .orElse(0L);
    }
}
