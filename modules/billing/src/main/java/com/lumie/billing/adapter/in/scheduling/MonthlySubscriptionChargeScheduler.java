package com.lumie.billing.adapter.in.scheduling;

import com.lumie.billing.application.port.out.BillingKeyPersistencePort;
import com.lumie.billing.application.port.out.InvoicePersistencePort;
import com.lumie.billing.application.port.out.PaymentGatewayPort;
import com.lumie.billing.application.port.out.SubscriptionPersistencePort;
import com.lumie.billing.application.service.InvoicePersistenceHelper;
import com.lumie.billing.application.service.PaymentTransactionLogger;
import com.lumie.billing.application.service.SubscriptionPersistenceHelper;
import com.lumie.billing.domain.entity.BillingKey;
import com.lumie.billing.domain.entity.Invoice;
import com.lumie.billing.domain.entity.Subscription;
import com.lumie.billing.domain.vo.Money;
import com.lumie.billing.domain.vo.TransactionDirection;
import com.lumie.billing.domain.vo.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Monthly subscription charge job.
 *
 * <p>Runs every 1st of month at 09:00 KST (00:00 UTC).
 * Finds all ACTIVE subscriptions with nextChargeAt overdue, charges via billing key,
 * creates an Invoice on success, or records a charge failure.
 *
 * <p>Strategy per HARD_RULES:
 * <ul>
 *   <li>PG calls ({@link PaymentGatewayPort#chargeWithBillingKey}) are made OUTSIDE @Transactional.</li>
 *   <li>DB state is committed via {@link InvoicePersistenceHelper} and {@link SubscriptionPersistenceHelper}.</li>
 *   <li>PaymentTransaction audit log is written REQUIRES_NEW in {@link PaymentTransactionLogger}.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlySubscriptionChargeScheduler {

    private static final DateTimeFormatter INVOICE_DATE_FORMAT = DateTimeFormatter
            .ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private final SubscriptionPersistencePort subscriptionPersistencePort;
    private final SubscriptionPersistenceHelper subscriptionPersistenceHelper;
    private final BillingKeyPersistencePort billingKeyPersistencePort;
    private final InvoicePersistencePort invoicePersistencePort;
    private final InvoicePersistenceHelper invoicePersistenceHelper;
    private final PaymentGatewayPort paymentGatewayPort;
    private final PaymentTransactionLogger transactionLogger;

    @Scheduled(cron = "0 0 0 1 * *", zone = "UTC")
    @SchedulerLock(name = "billing-monthly-charge", lockAtMostFor = "PT30M", lockAtLeastFor = "PT5M")
    public void chargeMonthlySubscriptions() {
        log.info("MonthlySubscriptionChargeScheduler: starting monthly charge run");

        List<Subscription> subscriptions = subscriptionPersistencePort
                .findActiveWithNextChargeAtOrBefore(Instant.now());

        log.info("MonthlySubscriptionChargeScheduler: found {} subscriptions to charge", subscriptions.size());

        int succeeded = 0;
        int failed = 0;

        for (Subscription subscription : subscriptions) {
            try {
                boolean chargedSuccessfully = chargeSubscription(subscription);
                if (chargedSuccessfully) {
                    succeeded++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                log.error("MonthlySubscriptionChargeScheduler: unexpected error for subscriptionId={}: {}",
                        subscription.getId(), e.getMessage(), e);
                failed++;
            }
        }

        log.info("MonthlySubscriptionChargeScheduler: completed. succeeded={}, failed={}", succeeded, failed);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private boolean chargeSubscription(Subscription subscription) {
        Long billingKeyId = subscription.getBillingKeyId();
        if (billingKeyId == null) {
            log.warn("Subscription has no billing key — skipping charge: subscriptionId={}", subscription.getId());
            subscriptionPersistenceHelper.recordChargeAttemptFailed(subscription.getId());
            return false;
        }

        BillingKey billingKey = billingKeyPersistencePort.findById(billingKeyId).orElse(null);
        if (billingKey == null || !billingKey.isActive()) {
            log.warn("BillingKey not active — skipping charge: subscriptionId={}, billingKeyId={}",
                    subscription.getId(), billingKeyId);
            subscriptionPersistenceHelper.recordChargeAttemptFailed(subscription.getId());
            return false;
        }

        Money chargeAmount = subscription.getPlan().getMonthlyPrice();
        if (chargeAmount == null || chargeAmount.isZero()) {
            log.debug("Free plan subscription — skipping charge: subscriptionId={}", subscription.getId());
            return true;
        }

        String orderId = generateOrderId(subscription);
        String orderName = "Lumie 구독 " + subscription.getPlanName();
        BigDecimal amount = BigDecimal.valueOf(chargeAmount.toLong());

        // Log PG request
        transactionLogger.log(subscription.getTenantId(), TransactionType.SUBSCRIPTION_CHARGE,
                TransactionDirection.REQUEST, orderId,
                "Subscription", subscription.getId(),
                Map.of("orderId", orderId, "amount", amount, "planId", subscription.getPlanId()),
                null);

        // PG call — outside @Transactional
        // customerKey must match what was stored at billing key issuance (BillingKey.customerKey).
        PaymentGatewayPort.BillingChargeResult result =
                paymentGatewayPort.chargeWithBillingKey(
                        billingKey.getPgBillingKey(),
                        billingKey.getCustomerKey(),
                        amount, orderId, orderName);

        transactionLogger.log(subscription.getTenantId(), TransactionType.SUBSCRIPTION_CHARGE,
                TransactionDirection.RESPONSE, orderId,
                "Subscription", subscription.getId(),
                Map.of("success", result.success(),
                        "paymentKey", result.paymentKey() != null ? result.paymentKey() : "",
                        "error", result.errorMessage() != null ? result.errorMessage() : ""),
                result.success() ? 200 : 400);

        if (result.success()) {
            createInvoiceAndAdvanceChargeCycle(subscription, orderId, chargeAmount, result.paymentKey());
            log.info("Monthly charge succeeded: subscriptionId={}, tenantId={}, amount={}",
                    subscription.getId(), subscription.getTenantId(), amount);
            return true;
        } else {
            subscriptionPersistenceHelper.recordChargeAttemptFailed(subscription.getId());
            log.error("Monthly charge failed: subscriptionId={}, tenantId={}, error={}",
                    subscription.getId(), subscription.getTenantId(), result.errorMessage());
            return false;
        }
    }

    private void createInvoiceAndAdvanceChargeCycle(Subscription subscription, String orderId,
                                                      Money chargeAmount, String paymentKey) {
        Instant now = Instant.now();
        Instant periodEnd = now.atZone(ZoneOffset.UTC).plusMonths(1).toInstant();

        Invoice invoice = Invoice.create(
                generateInvoiceNumber(subscription, now),
                subscription.getId(),
                subscription.getTenantId(),
                chargeAmount,
                Money.zero(),
                subscription.getPlanName() + " 월정액",
                now,
                periodEnd,
                periodEnd,
                orderId
        );

        Invoice saved = invoicePersistenceHelper.save(invoice);
        invoicePersistenceHelper.markPaid(saved.getId(), paymentKey, now);

        // Advance subscription nextChargeAt by one month
        Subscription latest = subscriptionPersistencePort.findById(subscription.getId()).orElse(subscription);
        latest.chargeSucceeded(periodEnd);
        subscriptionPersistenceHelper.save(latest);
    }

    private String generateOrderId(Subscription subscription) {
        return "SUB-%d-%s".formatted(subscription.getId(),
                UUID.randomUUID().toString().replace("-", "").substring(0, 12));
    }

    private String generateInvoiceNumber(Subscription subscription, Instant date) {
        return "INV-%d-%s-%s".formatted(
                subscription.getTenantId(),
                INVOICE_DATE_FORMAT.format(date),
                UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT));
    }
}
