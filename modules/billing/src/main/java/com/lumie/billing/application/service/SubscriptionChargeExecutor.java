package com.lumie.billing.application.service;

import com.lumie.billing.application.port.out.BillingKeyPersistencePort;
import com.lumie.billing.application.port.out.InvoicePersistencePort;
import com.lumie.billing.application.port.out.PaymentGatewayPort;
import com.lumie.billing.application.port.out.SubscriptionPersistencePort;
import com.lumie.billing.domain.entity.BillingKey;
import com.lumie.billing.domain.entity.Invoice;
import com.lumie.billing.domain.entity.Subscription;
import com.lumie.billing.domain.vo.Money;
import com.lumie.billing.domain.vo.TransactionDirection;
import com.lumie.billing.domain.vo.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Executes a single subscription charge via the configured PG.
 *
 * <p>Shared by {@link com.lumie.billing.adapter.in.scheduling.MonthlySubscriptionChargeScheduler}
 * (monthly recurring run) and {@link SubscriptionCommandService#chargeFirstInvoice} (first charge
 * triggered by user after subscribe + billing key registration).
 *
 * <p>No class-level {@code @Transactional} so PG call lives outside any transaction
 * (HARD_RULES: no external HTTP inside {@code @Transactional}). State changes are committed
 * via persistence helpers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionChargeExecutor {

    private static final DateTimeFormatter INVOICE_DATE_FORMAT = DateTimeFormatter
            .ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private final SubscriptionPersistencePort subscriptionPersistencePort;
    private final SubscriptionPersistenceHelper subscriptionPersistenceHelper;
    private final BillingKeyPersistencePort billingKeyPersistencePort;
    private final InvoicePersistencePort invoicePersistencePort;
    private final InvoicePersistenceHelper invoicePersistenceHelper;
    private final PaymentGatewayPort paymentGatewayPort;
    private final PaymentTransactionLogger transactionLogger;

    public ChargeOutcome charge(Subscription subscription) {
        Long billingKeyId = subscription.getBillingKeyId();
        if (billingKeyId == null) {
            log.warn("Subscription has no billing key — skipping charge: subscriptionId={}", subscription.getId());
            subscriptionPersistenceHelper.recordChargeAttemptFailed(subscription.getId());
            return ChargeOutcome.skipped("BILLING_KEY_NOT_ATTACHED");
        }

        BillingKey billingKey = billingKeyPersistencePort.findById(billingKeyId).orElse(null);
        if (billingKey == null || !billingKey.isActive()) {
            log.warn("BillingKey not active — skipping charge: subscriptionId={}, billingKeyId={}",
                    subscription.getId(), billingKeyId);
            subscriptionPersistenceHelper.recordChargeAttemptFailed(subscription.getId());
            return ChargeOutcome.skipped("BILLING_KEY_REVOKED");
        }

        Money chargeAmount = subscription.getPlan().getMonthlyPrice();
        if (chargeAmount == null || chargeAmount.isZero()) {
            log.debug("Free plan — no charge: subscriptionId={}", subscription.getId());
            return ChargeOutcome.skipped("FREE_PLAN");
        }

        String orderId = generateOrderId(subscription);
        String orderName = "Lumie 구독 " + subscription.getPlanName();
        BigDecimal amount = BigDecimal.valueOf(chargeAmount.toLong());

        transactionLogger.log(subscription.getTenantId(), TransactionType.SUBSCRIPTION_CHARGE,
                TransactionDirection.REQUEST, orderId,
                "Subscription", subscription.getId(),
                Map.of("orderId", orderId, "amount", amount, "planId", subscription.getPlanId()),
                null);

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
            Invoice invoice = createInvoiceAndAdvanceChargeCycle(
                    subscription, orderId, chargeAmount, result.paymentKey());
            log.info("Charge succeeded: subscriptionId={}, tenantId={}, amount={}",
                    subscription.getId(), subscription.getTenantId(), amount);
            return ChargeOutcome.paid(invoice);
        }

        subscriptionPersistenceHelper.recordChargeAttemptFailed(subscription.getId());
        log.error("Charge failed: subscriptionId={}, tenantId={}, error={}",
                subscription.getId(), subscription.getTenantId(), result.errorMessage());
        return ChargeOutcome.failed(result.errorMessage());
    }

    private Invoice createInvoiceAndAdvanceChargeCycle(Subscription subscription, String orderId,
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

        Subscription latest = subscriptionPersistencePort.findById(subscription.getId()).orElse(subscription);
        latest.chargeSucceeded(periodEnd);
        subscriptionPersistenceHelper.save(latest);

        return invoicePersistencePort.findById(saved.getId()).orElse(saved);
    }

    private static String generateOrderId(Subscription subscription) {
        return "SUB-%d-%s".formatted(subscription.getId(),
                UUID.randomUUID().toString().replace("-", "").substring(0, 12));
    }

    private static String generateInvoiceNumber(Subscription subscription, Instant date) {
        return "INV-%d-%s-%s".formatted(
                subscription.getTenantId(),
                INVOICE_DATE_FORMAT.format(date),
                UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT));
    }

    public record ChargeOutcome(Status status, Invoice invoice, String reason) {
        public enum Status { PAID, FAILED, SKIPPED }

        public static ChargeOutcome paid(Invoice invoice) {
            return new ChargeOutcome(Status.PAID, invoice, null);
        }

        public static ChargeOutcome failed(String reason) {
            return new ChargeOutcome(Status.FAILED, null, reason);
        }

        public static ChargeOutcome skipped(String reason) {
            return new ChargeOutcome(Status.SKIPPED, null, reason);
        }

        public boolean isSuccess() {
            return status == Status.PAID;
        }
    }
}
