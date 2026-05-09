package com.lumie.billing.application.service;

import com.lumie.billing.application.dto.request.PaddleWebhookPayload;
import com.lumie.billing.application.port.out.InvoicePersistencePort;
import com.lumie.billing.application.port.out.PlanPersistencePort;
import com.lumie.billing.application.port.out.SubscriptionPersistencePort;
import com.lumie.billing.application.port.out.WebhookIdempotencyPort;
import com.lumie.billing.domain.entity.Invoice;
import com.lumie.billing.domain.entity.Plan;
import com.lumie.billing.domain.entity.Subscription;
import com.lumie.billing.domain.vo.Money;
import com.lumie.internal.tenant.TenantInternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "paddle.enabled", havingValue = "true")
@Transactional
public class PaddleWebhookService {

    private final SubscriptionPersistencePort subscriptionPersistencePort;
    private final InvoicePersistencePort invoicePersistencePort;
    private final PlanPersistencePort planPersistencePort;
    private final WebhookIdempotencyPort webhookIdempotencyPort;
    private final TenantInternalService tenantInternalService;

    private static final String FREE_PLAN_ID = "FREE";

    public void handleWebhook(PaddleWebhookPayload payload) {
        if (webhookIdempotencyPort.isProcessed(payload.eventId())) {
            log.info("Duplicate webhook event ignored: {}", payload.eventId());
            return;
        }

        log.info("Processing Paddle webhook: eventType={}, eventId={}", payload.eventType(), payload.eventId());

        switch (payload.eventType()) {
            case "subscription.created" -> handleSubscriptionCreated(payload);
            case "subscription.updated" -> handleSubscriptionUpdated(payload);
            case "subscription.canceled" -> handleSubscriptionCanceled(payload);
            case "subscription.past_due" -> handleSubscriptionPastDue(payload);
            case "transaction.completed" -> handleTransactionCompleted(payload);
            case "transaction.payment_failed" -> handleTransactionFailed(payload);
            default -> log.info("Unhandled Paddle event type: {}", payload.eventType());
        }

        webhookIdempotencyPort.markProcessed(payload.eventId(), payload.eventType());
    }

    private void handleSubscriptionCreated(PaddleWebhookPayload payload) {
        String paddleSubId = payload.dataString("id");
        String paddleCustId = payload.dataString("customer_id");
        String tenantSlug = payload.customDataString("tenantSlug");
        String priceId = payload.firstPriceId();

        if (tenantSlug == null) {
            log.warn("No tenantSlug in subscription.created custom_data, paddleSubId={}", paddleSubId);
            return;
        }

        Plan plan = findPlanByPaddlePriceId(priceId);
        if (plan == null) {
            log.error("No plan found for Paddle priceId={}", priceId);
            return;
        }

        Subscription subscription = subscriptionPersistencePort.findByTenantSlug(tenantSlug)
                .map(existing -> {
                    existing.changePlan(plan);
                    existing.linkPaddle(paddleSubId, paddleCustId);
                    return existing;
                })
                .orElseGet(() -> {
                    log.warn("No existing subscription for tenant={}, creating new", tenantSlug);
                    Subscription newSub = Subscription.create(null, tenantSlug, plan,
                            LocalDateTime.now().plusMonths(1));
                    newSub.linkPaddle(paddleSubId, paddleCustId);
                    return newSub;
                });

        subscriptionPersistencePort.save(subscription);
        tenantInternalService.updatePlan(tenantSlug, plan.getId());
        log.info("Subscription created/linked: tenant={}, plan={}, paddleSub={}",
                tenantSlug, plan.getId(), paddleSubId);
    }

    private void handleSubscriptionUpdated(PaddleWebhookPayload payload) {
        String paddleSubId = payload.dataString("id");
        String priceId = payload.firstPriceId();

        Subscription subscription = findByPaddleSubId(paddleSubId);
        if (subscription == null) return;

        Plan plan = findPlanByPaddlePriceId(priceId);
        if (plan != null && !plan.getId().equals(subscription.getPlanId())) {
            subscription.changePlan(plan);
            subscriptionPersistencePort.save(subscription);
            tenantInternalService.updatePlan(subscription.getTenantSlug(), plan.getId());
            log.info("Subscription plan updated: paddleSub={}, newPlan={}", paddleSubId, plan.getId());
        }
    }

    private void handleSubscriptionCanceled(PaddleWebhookPayload payload) {
        String paddleSubId = payload.dataString("id");
        Subscription subscription = findByPaddleSubId(paddleSubId);
        if (subscription == null) return;

        subscription.cancel();
        subscriptionPersistencePort.save(subscription);
        tenantInternalService.updatePlan(subscription.getTenantSlug(), FREE_PLAN_ID);
        log.info("Subscription cancelled: paddleSub={}, tenant={}", paddleSubId, subscription.getTenantSlug());
    }

    private void handleSubscriptionPastDue(PaddleWebhookPayload payload) {
        String paddleSubId = payload.dataString("id");
        Subscription subscription = findByPaddleSubId(paddleSubId);
        if (subscription == null) return;

        subscription.markPastDue();
        subscriptionPersistencePort.save(subscription);
        log.info("Subscription past_due: paddleSub={}, tenant={}", paddleSubId, subscription.getTenantSlug());
    }

    private void handleTransactionCompleted(PaddleWebhookPayload payload) {
        String paddleTxnId = payload.dataString("id");
        String paddleSubId = payload.dataString("subscription_id");
        String amountStr = payload.transactionAmount();
        String currency = payload.transactionCurrency();

        Subscription subscription = findByPaddleSubId(paddleSubId);
        if (subscription == null) return;

        if (subscription.getStatus() == com.lumie.billing.domain.vo.SubscriptionStatus.PAST_DUE) {
            subscription.resumeFromPastDue();
        }

        long amount = amountStr != null ? Long.parseLong(amountStr) : 0;
        Invoice invoice = Invoice.create(
                "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                subscription,
                Money.of(amount),
                "Paddle transaction " + paddleTxnId,
                LocalDateTime.now().withDayOfMonth(1),
                LocalDateTime.now().plusMonths(1).withDayOfMonth(1),
                LocalDateTime.now(),
                paddleTxnId
        );
        invoice.markAsPaid(paddleTxnId);

        invoicePersistencePort.save(invoice);

        subscription.extendExpiration(LocalDateTime.now().plusMonths(1));
        subscriptionPersistencePort.save(subscription);

        log.info("Transaction completed: paddleTxn={}, tenant={}, amount={}",
                paddleTxnId, subscription.getTenantSlug(), amount);
    }

    private void handleTransactionFailed(PaddleWebhookPayload payload) {
        String paddleTxnId = payload.dataString("id");
        String paddleSubId = payload.dataString("subscription_id");

        Subscription subscription = findByPaddleSubId(paddleSubId);
        if (subscription == null) return;

        Invoice invoice = Invoice.create(
                "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                subscription,
                Money.of(0),
                "Failed Paddle transaction " + paddleTxnId,
                null, null, null, paddleTxnId
        );
        invoice.markAsFailed("Payment failed at Paddle");

        invoicePersistencePort.save(invoice);
        log.info("Transaction failed: paddleTxn={}, tenant={}", paddleTxnId, subscription.getTenantSlug());
    }

    private Subscription findByPaddleSubId(String paddleSubId) {
        return subscriptionPersistencePort.findByPaddleSubscriptionId(paddleSubId)
                .orElseGet(() -> {
                    log.warn("No subscription found for paddleSubId={}", paddleSubId);
                    return null;
                });
    }

    private Plan findPlanByPaddlePriceId(String priceId) {
        if (priceId == null) return null;
        return planPersistencePort.findAll().stream()
                .filter(p -> priceId.equals(p.getPaddleMonthlyPriceId())
                        || priceId.equals(p.getPaddleYearlyPriceId()))
                .findFirst()
                .orElse(null);
    }
}
