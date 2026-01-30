package com.lumie.billing.application.service;

import com.lumie.billing.application.dto.request.ChangeSubscriptionRequest;
import com.lumie.billing.application.dto.request.CreateSubscriptionRequest;
import com.lumie.billing.application.dto.response.SubscriptionResponse;
import com.lumie.billing.application.port.in.CancelSubscriptionUseCase;
import com.lumie.billing.application.port.in.ChangeSubscriptionUseCase;
import com.lumie.billing.application.port.in.CreateSubscriptionUseCase;
import com.lumie.billing.application.port.out.BillingEventPublisherPort;
import com.lumie.billing.application.port.out.PlanPersistencePort;
import com.lumie.billing.application.port.out.SubscriptionPersistencePort;
import com.lumie.billing.domain.entity.Plan;
import com.lumie.billing.domain.entity.Subscription;
import com.lumie.common.exception.DuplicateResourceException;
import com.lumie.common.exception.ResourceNotFoundException;
import com.lumie.messaging.event.SubscriptionCreatedEvent;
import com.lumie.messaging.event.SubscriptionUpgradedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionCommandService implements
        CreateSubscriptionUseCase,
        ChangeSubscriptionUseCase,
        CancelSubscriptionUseCase {

    private static final String FREE_PLAN_ID = "FREE";

    private final SubscriptionPersistencePort subscriptionPersistencePort;
    private final PlanPersistencePort planPersistencePort;
    private final BillingEventPublisherPort eventPublisherPort;

    @Override
    public SubscriptionResponse createSubscription(CreateSubscriptionRequest request) {
        log.info("Creating subscription for tenant: {} with plan: {}",
                request.tenantSlug(), request.planId());

        if (subscriptionPersistencePort.existsByTenantId(request.tenantId())) {
            throw new DuplicateResourceException("Subscription", "tenantId:" + request.tenantId());
        }

        Plan plan = planPersistencePort.findById(request.planId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan", request.planId()));

        LocalDateTime expiresAt = plan.isFree() ? null :
                LocalDateTime.now().plusMonths(request.billingCycleMonths());

        Subscription subscription = Subscription.create(
                request.tenantId(),
                request.tenantSlug(),
                plan,
                expiresAt
        );

        Subscription saved = subscriptionPersistencePort.save(subscription);
        log.info("Subscription created: id={}, tenantSlug={}, planId={}",
                saved.getId(), saved.getTenantSlug(), saved.getPlanId());

        publishSubscriptionCreatedEvent(saved);

        return SubscriptionResponse.from(saved);
    }

    @Override
    public SubscriptionResponse createFreeSubscription(Long tenantId, String tenantSlug) {
        log.info("Creating free subscription for tenant: {}", tenantSlug);

        if (subscriptionPersistencePort.existsByTenantId(tenantId)) {
            log.warn("Subscription already exists for tenant: {}", tenantSlug);
            return subscriptionPersistencePort.findByTenantId(tenantId)
                    .map(SubscriptionResponse::from)
                    .orElseThrow(() -> new ResourceNotFoundException("Subscription", "tenantId:" + tenantId));
        }

        Plan freePlan = planPersistencePort.findById(FREE_PLAN_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", FREE_PLAN_ID));

        Subscription subscription = Subscription.createFree(tenantId, tenantSlug, freePlan);
        Subscription saved = subscriptionPersistencePort.save(subscription);

        log.info("Free subscription created: id={}, tenantSlug={}", saved.getId(), saved.getTenantSlug());

        publishSubscriptionCreatedEvent(saved);

        return SubscriptionResponse.from(saved);
    }

    @Override
    public SubscriptionResponse changePlan(String tenantSlug, ChangeSubscriptionRequest request) {
        log.info("Changing plan for tenant: {} to plan: {}", tenantSlug, request.newPlanId());

        Subscription subscription = subscriptionPersistencePort.findActiveByTenantSlug(tenantSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", tenantSlug));

        String previousPlanId = subscription.getPlanId();

        Plan newPlan = planPersistencePort.findById(request.newPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan", request.newPlanId()));

        subscription.changePlan(newPlan);
        Subscription saved = subscriptionPersistencePort.save(subscription);

        log.info("Plan changed for tenant: {} from {} to {}",
                tenantSlug, previousPlanId, request.newPlanId());

        publishSubscriptionUpgradedEvent(saved, previousPlanId);

        return SubscriptionResponse.from(saved);
    }

    @Override
    public void cancelSubscription(String tenantSlug) {
        log.info("Cancelling subscription for tenant: {}", tenantSlug);

        Subscription subscription = subscriptionPersistencePort.findActiveByTenantSlug(tenantSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", tenantSlug));

        subscription.cancel();
        subscriptionPersistencePort.save(subscription);

        log.info("Subscription cancelled for tenant: {}", tenantSlug);
    }

    private void publishSubscriptionCreatedEvent(Subscription subscription) {
        SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(
                subscription.getTenantId(),
                subscription.getTenantSlug(),
                subscription.getId(),
                subscription.getPlanId(),
                subscription.getPlanName()
        );
        eventPublisherPort.publish(event);
    }

    private void publishSubscriptionUpgradedEvent(Subscription subscription, String previousPlanId) {
        SubscriptionUpgradedEvent event = new SubscriptionUpgradedEvent(
                subscription.getTenantId(),
                subscription.getTenantSlug(),
                subscription.getId(),
                previousPlanId,
                subscription.getPlanId(),
                subscription.getPlanName()
        );
        eventPublisherPort.publish(event);
    }
}
