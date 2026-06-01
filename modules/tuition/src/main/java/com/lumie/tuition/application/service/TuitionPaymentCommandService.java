package com.lumie.tuition.application.service;

import com.lumie.common.tenant.UserContextHolder;
import com.lumie.tuition.application.dto.request.RefundRequest;
import com.lumie.tuition.application.dto.request.RequestPaymentRequest;
import com.lumie.tuition.application.dto.response.TuitionPaymentResponse;
import com.lumie.tuition.application.port.out.TuitionBillingGatewayPort;
import com.lumie.tuition.domain.entity.Guardian;
import com.lumie.tuition.domain.entity.TuitionInvoice;
import com.lumie.tuition.domain.entity.TuitionPayment;
import com.lumie.tuition.domain.exception.TuitionErrorCode;
import com.lumie.tuition.domain.exception.TuitionException;
import com.lumie.tuition.domain.repository.GuardianRepository;
import com.lumie.tuition.domain.repository.TuitionInvoiceRepository;
import com.lumie.tuition.domain.repository.TuitionPaymentRepository;
import com.lumie.tuition.domain.vo.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TuitionPaymentCommandService {

    private final TuitionInvoiceRepository invoiceRepository;
    private final TuitionPaymentRepository paymentRepository;
    private final GuardianRepository guardianRepository;
    private final TuitionBillingGatewayPort billingGatewayPort;
    private final TuitionPaymentPersistenceHelper persistenceHelper;

    /**
     * Record a PENDING payment, commit, then dispatch the invoice via the billing
     * gateway (PaySsam) OUTSIDE the transaction (HARD_RULES: avoid pool exhaustion).
     * The gateway sends a mobile invoice to the guardian's phone; collection completes
     * asynchronously and is reconciled later via {@link #recordCollection}.
     */
    public TuitionPaymentResponse requestPayment(RequestPaymentRequest request, String idempotencyKey) {
        TuitionInvoice invoice = findInvoiceForPayment(request.invoiceId());

        String orderId = idempotencyKey != null ? generateOrderId(invoice.getId(), idempotencyKey)
                : generateOrderId(invoice.getId());

        // Resolve the guardian BEFORE persisting the PENDING row, so a missing guardian
        // cannot leave an orphaned committed PENDING payment.
        Guardian guardian = guardianRepository.findById(invoice.getGuardianId())
                .orElseThrow(() -> new TuitionException(TuitionErrorCode.GUARDIAN_NOT_FOUND,
                        "Guardian not found for invoice: " + invoice.getId()));

        TuitionPayment payment = persistenceHelper.savePendingPayment(invoice, orderId, request);

        TuitionBillingGatewayPort.SendInvoiceResult result = billingGatewayPort.sendInvoice(
                orderId,
                guardian.getPhone(),
                guardian.getName(),
                invoice.getTitle(),
                invoice.getTotalAmount().getAmount(),
                invoice.getDueDate()
        );

        if (!result.success()) {
            log.error("Gateway sendInvoice failed: orderId={}, error={}", orderId, result.errorMessage());
            persistenceHelper.failPendingPayment(payment.getId(), result.errorMessage());
            // Raw gateway message stays in the log only; do not surface vendor detail to the API.
            throw new TuitionException(TuitionErrorCode.INTERNAL_ERROR, "Invoice dispatch failed");
        }

        log.info("Invoice dispatched: paymentId={}, orderId={}, externalInvoiceId={}",
                payment.getId(), orderId, result.externalInvoiceId());
        return TuitionPaymentResponse.from(payment);
    }

    /**
     * Reconcile a completed collection reported by the billing gateway (future PaySsam
     * status callback). Captures the payment and marks the invoice PAID. Idempotent.
     *
     * <p>The caller (future PaySsam webhook adapter) MUST establish
     * {@link com.lumie.common.tenant.TenantContextHolder} before invoking — this method
     * writes to RLS-scoped tables and is blocked without an active tenant context.
     */
    @Transactional
    public TuitionPaymentResponse recordCollection(String orderId, String externalRef,
                                                   BigDecimal amount, Instant collectedAt) {
        TuitionPayment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new TuitionException(TuitionErrorCode.PAYMENT_NOT_FOUND,
                        "Payment not found for orderId: " + orderId));

        if (payment.isCaptured()) {
            log.warn("Duplicate recordCollection for orderId={} — already captured", orderId);
            return TuitionPaymentResponse.from(payment);
        }

        TuitionInvoice invoice = invoiceRepository.findById(payment.getTuitionInvoiceId())
                .orElseThrow(() -> new TuitionException(TuitionErrorCode.INVOICE_NOT_FOUND,
                        "Invoice not found for paymentId: " + payment.getId()));

        if (payment.getAmount().getAmount().compareTo(amount) != 0) {
            log.error("Amount mismatch: expected={}, got={}, orderId={}", payment.getAmount(), amount, orderId);
            payment.fail("Amount mismatch from gateway callback");
            paymentRepository.save(payment);
            throw new TuitionException(TuitionErrorCode.PAYMENT_AMOUNT_MISMATCH,
                    "Gateway amount does not match expected payment amount");
        }

        payment.capture(externalRef, collectedAt);
        paymentRepository.save(payment);
        invoice.markPaid(collectedAt);
        invoiceRepository.save(invoice);

        log.info("Collection recorded: paymentId={}, orderId={}, invoiceId={}",
                payment.getId(), orderId, invoice.getId());
        return TuitionPaymentResponse.from(payment);
    }

    /**
     * Record a refund against a payment. Under the PaySsam model the actual refund to
     * the guardian is a gateway-side (merchant-of-record) process; here we only record
     * the domain refund so it is reflected in Lumie's books.
     */
    public TuitionPaymentResponse refundPayment(Long paymentId, RefundRequest request) {
        Long staffId = UserContextHolder.getUserId();
        Money refundAmount = Money.of(request.amount());

        TuitionPayment payment = persistenceHelper.applyRefundToPayment(
                paymentId, refundAmount, request.reason(), staffId, TuitionErrorCode.PAYMENT_NOT_FOUND);

        log.info("Payment refunded (recorded): paymentId={}, amount={}, staffId={}",
                paymentId, request.amount(), staffId);
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
        // Deterministic suffix so duplicate requests produce the same orderId.
        String suffix = idempotencyKey.replace("-", "");
        String truncated = suffix.length() >= 12 ? suffix.substring(0, 12) : suffix;
        return "TUITION-%d-%s".formatted(invoiceId, truncated);
    }
}
