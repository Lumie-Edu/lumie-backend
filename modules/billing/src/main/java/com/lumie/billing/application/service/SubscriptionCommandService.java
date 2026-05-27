package com.lumie.billing.application.service;

import com.lumie.billing.adapter.out.config.BillingProperties;
import com.lumie.billing.application.dto.request.ChangeSubscriptionRequest;
import com.lumie.billing.application.dto.request.CreateSubscriptionRequest;
import com.lumie.billing.application.dto.response.SubscriptionResponse;
import com.lumie.billing.application.port.out.PlanPersistencePort;
import com.lumie.billing.application.port.out.SubscriptionPersistencePort;
import com.lumie.billing.domain.entity.Plan;
import com.lumie.billing.domain.entity.Subscription;
import com.lumie.billing.domain.exception.BillingErrorCode;
import com.lumie.billing.domain.exception.BillingException;
import com.lumie.billing.domain.vo.SuspensionReason;
import com.lumie.common.exception.DuplicateResourceException;
import com.lumie.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionCommandService {

    private final BillingProperties billingProperties;

    private final SubscriptionPersistencePort subscriptionPersistencePort;
    private final PlanPersistencePort planPersistencePort;

    public SubscriptionResponse createSubscription(CreateSubscriptionRequest request) {
        log.info("Creating subscription for tenant: {} with plan: {}",
                request.tenantSlug(), request.planId());

        if (subscriptionPersistencePort.existsByTenantId(request.tenantId())) {
            throw new DuplicateResourceException("Subscription", "tenantId:" + request.tenantId());
        }

        Plan plan = planPersistencePort.findById(request.planId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan", request.planId()));

        Instant expiresAt = plan.isFree() ? null :
                Instant.now().atZone(ZoneOffset.UTC).plusMonths(request.billingCycleMonths()).toInstant();

        Subscription subscription = Subscription.create(
                request.tenantId(), request.tenantSlug(), plan, expiresAt);

        Subscription saved = subscriptionPersistencePort.save(subscription);
        log.info("Subscription created: id={}, tenantSlug={}, planId={}",
                saved.getId(), saved.getTenantSlug(), saved.getPlanId());

        return SubscriptionResponse.from(saved);
    }

    public SubscriptionResponse createFreeSubscription(Long tenantId, String tenantSlug) {
        log.info("Creating free subscription for tenant: {}", tenantSlug);

        if (subscriptionPersistencePort.existsByTenantId(tenantId)) {
            log.warn("Subscription already exists for tenant: {}", tenantSlug);
            return subscriptionPersistencePort.findByTenantId(tenantId)
                    .map(SubscriptionResponse::from)
                    .orElseThrow(() -> new ResourceNotFoundException("Subscription", "tenantId:" + tenantId));
        }

        String freePlanId = billingProperties.subscription().freePlanId();
        Plan freePlan = planPersistencePort.findById(freePlanId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", freePlanId));

        Subscription subscription = Subscription.createFree(tenantId, tenantSlug, freePlan);
        Subscription saved = subscriptionPersistencePort.save(subscription);

        log.info("Free subscription created: id={}, tenantSlug={}", saved.getId(), saved.getTenantSlug());
        return SubscriptionResponse.from(saved);
    }

    public SubscriptionResponse changePlan(String tenantSlug, ChangeSubscriptionRequest request) {
        log.info("Changing plan for tenant: {} to plan: {}", tenantSlug, request.newPlanId());

        Subscription subscription = subscriptionPersistencePort.findActiveByTenantSlug(tenantSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", tenantSlug));

        String previousPlanId = subscription.getPlanId();

        Plan newPlan = planPersistencePort.findById(request.newPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan", request.newPlanId()));

        subscription.changePlan(newPlan);
        Subscription saved = subscriptionPersistencePort.save(subscription);

        log.info("Plan changed for tenant: {} from {} to {}", tenantSlug, previousPlanId, request.newPlanId());
        return SubscriptionResponse.from(saved);
    }

    public void cancelSubscription(String tenantSlug) {
        log.info("Cancelling subscription for tenant: {}", tenantSlug);

        Subscription subscription = subscriptionPersistencePort.findByTenantSlug(tenantSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", tenantSlug));

        subscription.cancel();
        subscriptionPersistencePort.save(subscription);
        log.info("Subscription cancelled for tenant: {}", tenantSlug);
    }

    public void pauseSubscription(String tenantSlug, SuspensionReason reason) {
        log.info("Pausing subscription for tenant: {}, reason={}", tenantSlug, reason);

        Subscription subscription = subscriptionPersistencePort.findByTenantSlug(tenantSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", tenantSlug));

        subscription.pause(reason);
        subscriptionPersistencePort.save(subscription);
        log.info("Subscription paused for tenant: {}", tenantSlug);
    }

    public void resumeSubscription(String tenantSlug) {
        log.info("Resuming subscription for tenant: {}", tenantSlug);

        Subscription subscription = subscriptionPersistencePort.findByTenantSlug(tenantSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", tenantSlug));

        subscription.resume();
        subscriptionPersistencePort.save(subscription);
        log.info("Subscription resumed for tenant: {}", tenantSlug);
    }
}
