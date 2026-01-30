package com.lumie.billing.application.service;

import com.lumie.billing.application.dto.request.ConfirmPaymentRequest;
import com.lumie.billing.application.dto.request.TossWebhookPayload;
import com.lumie.billing.application.dto.response.InvoiceResponse;
import com.lumie.billing.application.dto.response.PaymentResponse;
import com.lumie.billing.application.port.in.ConfirmPaymentUseCase;
import com.lumie.billing.application.port.in.GetPaymentHistoryUseCase;
import com.lumie.billing.application.port.in.HandleWebhookUseCase;
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

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService implements
        ConfirmPaymentUseCase,
        HandleWebhookUseCase,
        GetPaymentHistoryUseCase {

    private final PaymentGatewayPort paymentGatewayPort;
    private final InvoicePersistencePort invoicePersistencePort;
    private final SubscriptionPersistencePort subscriptionPersistencePort;

    @Override
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

    @Override
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

    @Override
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
            LocalDateTime newExpiresAt = calculateNewExpiration(invoice);
            subscription.extendExpiration(newExpiresAt);
            subscriptionPersistencePort.save(subscription);
            log.info("Subscription extended for tenant: {}, newExpiresAt={}",
                    subscription.getTenantSlug(), newExpiresAt);
        }
    }

    private LocalDateTime calculateNewExpiration(Invoice invoice) {
        LocalDateTime baseDate = invoice.getSubscription().getExpiresAt();
        if (baseDate == null || baseDate.isBefore(LocalDateTime.now())) {
            baseDate = LocalDateTime.now();
        }

        if (invoice.getBillingPeriodStart() != null && invoice.getBillingPeriodEnd() != null) {
            long months = java.time.temporal.ChronoUnit.MONTHS.between(
                    invoice.getBillingPeriodStart(), invoice.getBillingPeriodEnd());
            return baseDate.plusMonths(Math.max(1, months));
        }

        return baseDate.plusMonths(1);
    }
}
