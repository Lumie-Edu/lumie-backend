package com.lumie.tuition.domain.entity;

import com.lumie.tuition.domain.vo.Money;
import com.lumie.common.domain.TenantScopedEntity;
import com.lumie.tuition.domain.exception.TuitionErrorCode;
import com.lumie.tuition.domain.exception.TuitionException;
import com.lumie.tuition.domain.vo.TuitionPaymentMethod;
import com.lumie.tuition.domain.vo.TuitionPaymentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "tuition_payments", indexes = {
        @Index(name = "idx_tuition_payments_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_tuition_payments_invoice_id", columnList = "tuition_invoice_id"),
        @Index(name = "idx_tuition_payments_order_id", columnList = "order_id", unique = true),
        @Index(name = "idx_tuition_payments_pg_transaction_id", columnList = "pg_transaction_id"),
        @Index(name = "idx_tuition_payments_status", columnList = "status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TuitionPayment extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "tuition_invoice_id", nullable = false)
    private Long tuitionInvoiceId;

    @Column(name = "pg_transaction_id", length = 200)
    private String pgTransactionId;

    @Column(name = "order_id", nullable = false, unique = true, length = 100)
    private String orderId;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "amount", nullable = false))
    private Money amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private TuitionPaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TuitionPaymentStatus status;

    @Column(name = "captured_at")
    private Instant capturedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "refunded_amount", nullable = false))
    })
    private Money refundedAmount;

    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @Column(name = "refunded_by_staff_id")
    private Long refundedByStaffId;

    @Builder
    private TuitionPayment(Long tuitionInvoiceId, String orderId, Money amount,
                            TuitionPaymentMethod method) {
        this.tuitionInvoiceId = tuitionInvoiceId;
        this.orderId = orderId;
        this.amount = amount;
        this.method = method;
        this.status = TuitionPaymentStatus.PENDING;
        this.refundedAmount = Money.zero();
    }

    public static TuitionPayment create(Long tuitionInvoiceId, String orderId,
                                         Money amount, TuitionPaymentMethod method) {
        return TuitionPayment.builder()
                .tuitionInvoiceId(tuitionInvoiceId)
                .orderId(orderId)
                .amount(amount)
                .method(method)
                .build();
    }

    public void capture(String pgTransactionId, Instant capturedAt) {
        if (this.status != TuitionPaymentStatus.PENDING) {
            throw new TuitionException(TuitionErrorCode.INVALID_PAYMENT_STATUS,
                    "Payment can only be captured when PENDING");
        }
        this.pgTransactionId = pgTransactionId;
        this.status = TuitionPaymentStatus.CAPTURED;
        this.capturedAt = capturedAt;
    }

    public void fail(String reason) {
        if (this.status != TuitionPaymentStatus.PENDING) {
            return;
        }
        this.status = TuitionPaymentStatus.FAILED;
        this.failedAt = Instant.now();
        this.failureReason = reason;
    }

    public void refund(Money refundAmount, String reason, Long staffId) {
        if (this.status != TuitionPaymentStatus.CAPTURED) {
            throw new TuitionException(TuitionErrorCode.INVALID_PAYMENT_STATUS,
                    "Only CAPTURED payments can be refunded");
        }
        Money newRefundedAmount = this.refundedAmount.add(refundAmount);
        if (newRefundedAmount.isGreaterThan(this.amount)) {
            throw new TuitionException(TuitionErrorCode.REFUND_EXCEEDS_PAYMENT,
                    "Refund amount exceeds original payment amount");
        }
        this.refundedAmount = newRefundedAmount;
        this.refundReason = reason;
        this.refundedAt = Instant.now();
        this.refundedByStaffId = staffId;

        if (this.refundedAmount.equals(this.amount)) {
            this.status = TuitionPaymentStatus.CANCELLED;
        }
    }

    public boolean isCaptured() {
        return this.status == TuitionPaymentStatus.CAPTURED;
    }
}
