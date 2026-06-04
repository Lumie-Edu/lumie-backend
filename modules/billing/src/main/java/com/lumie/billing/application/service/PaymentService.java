package com.lumie.billing.application.service;

import com.lumie.billing.application.dto.request.ConfirmPaymentRequest;
import com.lumie.billing.application.dto.response.InvoiceResponse;
import com.lumie.billing.application.port.out.InvoicePersistencePort;
import com.lumie.billing.application.port.out.PaymentGatewayPort;
import com.lumie.billing.application.port.out.SubscriptionPersistencePort;
import com.lumie.billing.domain.entity.Invoice;
import com.lumie.billing.domain.exception.PaymentFailedException;
import com.lumie.billing.domain.vo.TransactionDirection;
import com.lumie.billing.domain.vo.TransactionType;
import com.lumie.common.api.PageResponse;
import com.lumie.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Handles one-time Toss Payments confirmations for subscription invoices.
 * No class-level @Transactional — PG calls must stay outside transaction boundaries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentGatewayPort paymentGatewayPort;
    private final InvoicePersistencePort invoicePersistencePort;
    private final InvoicePersistenceHelper invoicePersistenceHelper;
    private final SubscriptionPersistencePort subscriptionPersistencePort;
    private final SubscriptionPersistenceHelper subscriptionPersistenceHelper;
    private final PaymentTransactionLogger transactionLogger;

    /**
     * Confirms a one-time payment: verifies amount, calls PG, updates Invoice.
     * PG call is OUTSIDE @Transactional per HARD_RULES.
     */
    public InvoiceResponse confirmPayment(ConfirmPaymentRequest request) {
        log.info("Confirming payment: orderId={}, amount={}", request.orderId(), request.amount());

        Invoice invoice = invoicePersistencePort.findByOrderId(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "orderId:" + request.orderId()));

        if (invoice.getAmount().toLong() != request.amount()) {
            throw new PaymentFailedException("Amount mismatch: expected " +
                    invoice.getAmount().toLong() + ", got " + request.amount());
        }

        // Log PG request
        transactionLogger.log(invoice.getTenantId(), TransactionType.SUBSCRIPTION_CHARGE,
                TransactionDirection.REQUEST, request.orderId(),
                "Invoice", invoice.getId(),
                Map.of("orderId", request.orderId(), "paymentKey", request.paymentKey(),
                        "amount", request.amount()),
                null);

        // PG call — outside @Transactional
        PaymentGatewayPort.PaymentConfirmResult result =
                paymentGatewayPort.confirmPayment(request.paymentKey(), request.orderId(),
                        BigDecimal.valueOf(request.amount()));

        transactionLogger.log(invoice.getTenantId(), TransactionType.SUBSCRIPTION_CHARGE,
                TransactionDirection.RESPONSE, request.orderId(),
                "Invoice", invoice.getId(),
                Map.of("success", result.success(),
                        "paymentKey", result.paymentKey() != null ? result.paymentKey() : "",
                        "error", result.errorMessage() != null ? result.errorMessage() : ""),
                result.success() ? 200 : 400);

        if (result.success()) {
            invoicePersistenceHelper.markPaid(invoice.getId(), request.paymentKey(), Instant.now());
            log.info("Payment confirmed: orderId={}, paymentKey={}", request.orderId(), request.paymentKey());
        } else {
            invoicePersistenceHelper.markFailed(invoice.getId(), result.errorMessage());
            log.error("Payment failed: orderId={}, reason={}", request.orderId(), result.errorMessage());
            throw new PaymentFailedException(result.errorMessage(), request.paymentKey(), null);
        }

        return invoicePersistencePort.findById(invoice.getId())
                .map(InvoiceResponse::from)
                .orElseThrow();
    }

    @Transactional(readOnly = true)
    public PageResponse<InvoiceResponse> getPaymentHistory(Long tenantId, Pageable pageable) {
        log.debug("Fetching payment history for tenantId: {}", tenantId);
        return PageResponse.from(
                invoicePersistencePort.findByTenantId(tenantId, pageable).map(InvoiceResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<InvoiceResponse> getPaymentHistoryBySlug(String tenantSlug, Pageable pageable) {
        log.debug("Fetching payment history for tenantSlug: {}", tenantSlug);
        return PageResponse.from(
                invoicePersistencePort.findByTenantSlug(tenantSlug, pageable).map(InvoiceResponse::from)
        );
    }
}
