package com.lumie.billing.application.service;

import com.lumie.billing.application.dto.request.ConfirmPaymentRequest;
import com.lumie.billing.application.dto.request.TossWebhookPayload;
import com.lumie.billing.application.dto.response.InvoiceResponse;
import com.lumie.billing.application.dto.response.PaymentResponse;
import com.lumie.billing.application.port.out.InvoicePersistencePort;
import com.lumie.billing.application.port.out.PaymentGatewayPort;
import com.lumie.billing.application.port.out.SubscriptionPersistencePort;
import com.lumie.billing.domain.entity.Invoice;
import com.lumie.billing.domain.entity.Subscription;
import com.lumie.billing.domain.exception.PaymentFailedException;
import com.lumie.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentGatewayPort paymentGatewayPort;
    private final InvoicePersistencePort invoicePersistencePort;
    private final SubscriptionPersistencePort subscriptionPersistencePort;

    public PaymentResponse confirmPayment(ConfirmPaymentRequest request) {
        log.info("Confirming payment: orderId={}, amount={}", request.orderId(), request.amount());

        Invoice invoice = invoicePersistencePort.findByOrderId(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "orderId:" + request.orderId()));

        if (invoice.getAmount().toLong() != request.amount()) {
            throw new PaymentFailedException("Amount mismatch: expected " +
                    invoice.getAmount().toLong() + ", got " + request.amount());
        }

        PaymentResponse response = paymentGatewayPort.confirmPayment(
                request.paymentKey(), request.orderId(), request.amount());

        if (response.isSuccess()) {
            invoice.markAsPaid(request.paymentKey());
            invoicePersistencePort.save(invoice);

            extendSubscription(invoice);

            log.info("Payment confirmed successfully: orderId={}, paymentKey={}",
                    request.orderId(), request.paymentKey());
        } else {
            invoice.markAsFailed(response.message());
            invoicePersistencePort.save(invoice);
            log.error("Payment failed: orderId={}, reason={}", request.orderId(), response.message());
        }

        return response;
    }

    public void handleWebhook(TossWebhookPayload payload) {
        log.info("Processing webhook: eventType={}, orderId={}",
                payload.eventType(), payload.data().orderId());

        Invoice invoice = invoicePersistencePort.findByOrderId(payload.data().orderId())
                .orElse(null);

        if (invoice == null) {
            log.warn("Invoice not found for orderId: {}", payload.data().orderId());
            return;
        }

        if (payload.isPaymentDone() && invoice.isPending()) {
            invoice.markAsPaid(payload.data().paymentKey());
            invoicePersistencePort.save(invoice);
            extendSubscription(invoice);
            log.info("Payment marked as paid via webhook: orderId={}", payload.data().orderId());
        } else if (payload.isPaymentCanceled() && invoice.isPaid()) {
            invoice.refund();
            invoicePersistencePort.save(invoice);
            log.info("Payment refunded via webhook: orderId={}", payload.data().orderId());
        }
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getPaymentHistory(String tenantSlug) {
        log.debug("Fetching payment history for tenant: {}", tenantSlug);
        return invoicePersistencePort.findByTenantSlug(tenantSlug).stream()
                .map(InvoiceResponse::from)
                .toList();
    }

    private void extendSubscription(Invoice invoice) {
        Subscription subscription = invoice.getSubscription();
        if (subscription != null && subscription.isActive()) {
            Instant newExpiresAt = calculateNewExpiration(invoice);
            subscription.extendExpiration(newExpiresAt);
            subscriptionPersistencePort.save(subscription);
            log.info("Subscription extended for tenant: {}, newExpiresAt={}",
                    subscription.getTenantSlug(), newExpiresAt);
        }
    }

    private Instant calculateNewExpiration(Invoice invoice) {
        Instant now = Instant.now();
        Instant baseDate = invoice.getSubscription().getExpiresAt();
        if (baseDate == null || baseDate.isBefore(now)) {
            baseDate = now;
        }

        long months = 1;
        if (invoice.getBillingPeriodStart() != null && invoice.getBillingPeriodEnd() != null) {
            months = Math.max(1, java.time.temporal.ChronoUnit.MONTHS.between(
                    invoice.getBillingPeriodStart().atZone(ZoneOffset.UTC),
                    invoice.getBillingPeriodEnd().atZone(ZoneOffset.UTC)));
        }
        return baseDate.atZone(ZoneOffset.UTC).plusMonths(months).toInstant();
    }
}
