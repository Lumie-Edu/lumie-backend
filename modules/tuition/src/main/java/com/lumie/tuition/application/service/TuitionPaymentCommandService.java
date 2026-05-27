package com.lumie.tuition.application.service;

import com.lumie.tuition.domain.vo.Money;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.tuition.application.dto.request.RefundRequest;
import com.lumie.tuition.application.dto.request.RequestPaymentRequest;
import com.lumie.tuition.application.dto.response.PaymentCheckoutResponse;
import com.lumie.tuition.application.dto.response.TuitionPaymentResponse;
import com.lumie.tuition.application.port.out.TuitionPaymentGatewayPort;
import com.lumie.tuition.domain.entity.TuitionInvoice;
import com.lumie.tuition.domain.entity.TuitionPayment;
import com.lumie.tuition.domain.exception.TuitionErrorCode;
import com.lumie.tuition.domain.exception.TuitionException;
import com.lumie.tuition.domain.repository.TuitionInvoiceRepository;
import com.lumie.tuition.domain.repository.TuitionPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TuitionPaymentCommandService {

    private final TuitionInvoiceRepository invoiceRepository;
    private final TuitionPaymentRepository paymentRepository;
    private final TuitionPaymentGatewayPort paymentGatewayPort;
    private final TuitionPaymentPersistenceHelper persistenceHelper;

    /**
     * Step 1: Record a PENDING payment, commit, then call PG outside the transaction.
     * PG call MUST be outside @Transactional (HARD_RULES: avoid pool exhaustion).
     */
    public PaymentCheckoutResponse requestPayment(RequestPaymentRequest request, String idempotencyKey) {
        TuitionInvoice invoice = findInvoiceForPayment(request.invoiceId());

        String orderId = idempotencyKey != null ? generateOrderId(invoice.getId(), idempotencyKey)
                : generateOrderId(invoice.getId());

        // Step 1a: persist PENDING payment row inside a separate transaction (via helper bean)
        TuitionPayment payment = persistenceHelper.savePendingPayment(invoice, orderId, request);

        // Step 1b: call PG outside any @Transactional boundary
        TuitionPaymentGatewayPort.TuitionPaymentRequest pgRequest =
                new TuitionPaymentGatewayPort.TuitionPaymentRequest(
                        orderId,
                        invoice.getTitle(),
                        invoice.getTotalAmount().getAmount(),
                        request.customerName(),
                        request.customerEmail(),
                        request.successUrl(),
                        request.failUrl()
                );
        TuitionPaymentGatewayPort.PaymentRequestResult result = paymentGatewayPort.requestPayment(pgRequest);

        if (!result.success()) {
            log.error("PG requestPayment failed: orderId={}, error={}", orderId, result.errorMessage());
            persistenceHelper.failPendingPayment(payment.getId(), result.errorMessage());
            throw new TuitionException(TuitionErrorCode.INTERNAL_ERROR,
                    "Payment request failed: " + result.errorMessage());
        }

        log.info("Payment requested: paymentId={}, orderId={}", payment.getId(), orderId);
        return new PaymentCheckoutResponse(payment.getId(), orderId, result.checkoutUrl());
    }

    /**
     * Called from the Toss webhook — confirm a payment that completed on PG side.
     * Step 1: verify webhook signature (already done in controller), parse orderId.
     * Step 2: update TuitionPayment and TuitionInvoice in a single transaction.
     */
    @Transactional
    public void confirmPaymentFromWebhook(String orderId, String pgTransactionId,
                                           java.math.BigDecimal amount, Instant capturedAt) {
        TuitionPayment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new TuitionException(TuitionErrorCode.PAYMENT_NOT_FOUND,
                        "Payment not found for orderId: " + orderId));

        if (payment.isCaptured()) {
            log.warn("Duplicate webhook for orderId={} — payment already captured", orderId);
            return;
        }

        TuitionInvoice invoice = invoiceRepository.findById(payment.getTuitionInvoiceId())
                .orElseThrow(() -> new TuitionException(TuitionErrorCode.INVOICE_NOT_FOUND,
                        "Invoice not found for paymentId: " + payment.getId()));

        if (payment.getAmount().getAmount().compareTo(amount) != 0) {
            log.error("Amount mismatch: expected={}, got={}, orderId={}", payment.getAmount(), amount, orderId);
            payment.fail("Amount mismatch from PG webhook");
            paymentRepository.save(payment);
            throw new TuitionException(TuitionErrorCode.PAYMENT_AMOUNT_MISMATCH,
                    "PG amount does not match expected payment amount");
        }

        payment.capture(pgTransactionId, capturedAt);
        paymentRepository.save(payment);

        invoice.markPaid(capturedAt);
        invoiceRepository.save(invoice);

        log.info("Payment confirmed via webhook: paymentId={}, orderId={}, invoiceId={}",
                payment.getId(), orderId, invoice.getId());
    }

    public TuitionPaymentResponse refundPayment(Long paymentId, RefundRequest request) {
        Long staffId = UserContextHolder.getUserId();
        Money refundAmount = Money.of(request.amount());

        // Step 1: record refund intent in DB (via helper bean — separate transaction, commits before PG)
        TuitionPayment payment = persistenceHelper.applyRefundToPayment(
                paymentId, refundAmount, request.reason(), staffId, TuitionErrorCode.PAYMENT_NOT_FOUND);

        // Step 2: call PG outside @Transactional
        TuitionPaymentGatewayPort.RefundResult pgResult = paymentGatewayPort.refund(
                payment.getPgTransactionId(),
                request.amount(),
                request.reason(),
                UUID.randomUUID().toString()
        );

        if (!pgResult.success()) {
            log.error("PG refund failed: paymentId={}, error={}", paymentId, pgResult.errorMessage());
            throw new TuitionException(TuitionErrorCode.INTERNAL_ERROR,
                    "Refund failed: " + pgResult.errorMessage());
        }

        log.info("Payment refunded: paymentId={}, amount={}, staffId={}", paymentId, request.amount(), staffId);
        return TuitionPaymentResponse.from(payment);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private TuitionInvoice findInvoiceForPayment(Long invoiceId) {
        TuitionInvoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new TuitionException(TuitionErrorCode.INVOICE_NOT_FOUND,
                        "TuitionInvoice not found: " + invoiceId));
        if (!invoice.isIssuedOrOverdue()) {
            throw new TuitionException(TuitionErrorCode.INVALID_INVOICE_STATUS,
                    "Invoice must be ISSUED or OVERDUE to initiate payment");
        }
        return invoice;
    }

    private String generateOrderId(Long invoiceId) {
        return "TUITION-%d-%s".formatted(invoiceId, UUID.randomUUID().toString().replace("-", "").substring(0, 12));
    }

    private String generateOrderId(Long invoiceId, String idempotencyKey) {
        // Derive a deterministic suffix from the idempotency key so duplicate requests
        // produce the same orderId and can be detected at the payment gateway level.
        String suffix = idempotencyKey.replace("-", "");
        String truncated = suffix.length() >= 12 ? suffix.substring(0, 12) : suffix;
        return "TUITION-%d-%s".formatted(invoiceId, truncated);
    }
}
