package com.lumie.billing.domain.entity;

import com.lumie.billing.domain.exception.BillingErrorCode;
import com.lumie.billing.domain.exception.BillingException;
import com.lumie.billing.domain.vo.InvoiceStatus;
import com.lumie.billing.domain.vo.Money;
import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

/**
 * Platform-scoped billing invoice (no RLS).
 *
 * <p>OVERDUE is a computed state, not a stored enum value — use {@link #isOverdue()} which
 * returns {@code true} when status=PENDING and dueDate has passed. This avoids a persistent
 * state machine transition that would require a scheduler update path.
 */
@Entity
@Table(name = "invoices", indexes = {
        @Index(name = "idx_invoices_subscription_id", columnList = "subscription_id"),
        @Index(name = "idx_invoices_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_invoices_status", columnList = "status"),
        @Index(name = "idx_invoices_invoice_number", columnList = "invoice_number", unique = true),
        @Index(name = "idx_invoices_order_id", columnList = "order_id", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    @Column(name = "subscription_id", nullable = false)
    private Long subscriptionId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "amount", nullable = false))
    private Money amount;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "vat_amount", nullable = false))
    })
    private Money vatAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InvoiceStatus status;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "billing_period_start")
    private Instant billingPeriodStart;

    @Column(name = "billing_period_end")
    private Instant billingPeriodEnd;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "payment_key", length = 200)
    private String paymentKey;

    @Column(name = "order_id", length = 100, unique = true)
    private String orderId;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Builder
    private Invoice(String invoiceNumber, Long subscriptionId, Long tenantId,
                    Money amount, Money vatAmount, String description,
                    Instant billingPeriodStart, Instant billingPeriodEnd,
                    Instant dueDate, String orderId) {
        this.invoiceNumber = invoiceNumber;
        this.subscriptionId = subscriptionId;
        this.tenantId = tenantId;
        this.amount = amount;
        this.vatAmount = vatAmount != null ? vatAmount : Money.zero();
        this.status = InvoiceStatus.PENDING;
        this.description = description;
        this.billingPeriodStart = billingPeriodStart;
        this.billingPeriodEnd = billingPeriodEnd;
        this.dueDate = dueDate;
        this.orderId = orderId;
    }

    public static Invoice create(String invoiceNumber, Long subscriptionId, Long tenantId,
                                  Money amount, Money vatAmount, String description,
                                  Instant billingPeriodStart, Instant billingPeriodEnd,
                                  Instant dueDate, String orderId) {
        return Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .subscriptionId(subscriptionId)
                .tenantId(tenantId)
                .amount(amount)
                .vatAmount(vatAmount)
                .description(description)
                .billingPeriodStart(billingPeriodStart)
                .billingPeriodEnd(billingPeriodEnd)
                .dueDate(dueDate)
                .orderId(orderId)
                .build();
    }

    // ─── Domain methods ───────────────────────────────────────────────────────

    public void markPaid(String paymentKey, Instant paidAt) {
        if (this.status != InvoiceStatus.PENDING) {
            throw new BillingException(BillingErrorCode.INVALID_INVOICE_STATE,
                    "Invoice must be PENDING to be marked paid. Current: " + this.status);
        }
        this.status = InvoiceStatus.PAID;
        this.paidAt = paidAt != null ? paidAt : Instant.now();
        this.paymentKey = paymentKey;
    }

    public void markFailed(String reason) {
        if (this.status != InvoiceStatus.PENDING) {
            return;
        }
        this.status = InvoiceStatus.FAILED;
        this.failureReason = reason;
    }

    public void markRefunded() {
        if (this.status != InvoiceStatus.PAID) {
            throw new BillingException(BillingErrorCode.INVALID_INVOICE_STATE,
                    "Only PAID invoices can be refunded. Current: " + this.status);
        }
        this.status = InvoiceStatus.REFUNDED;
    }

    // ─── Query methods ────────────────────────────────────────────────────────

    public boolean isPaid() {
        return this.status == InvoiceStatus.PAID;
    }

    public boolean isPending() {
        return this.status == InvoiceStatus.PENDING;
    }

    /**
     * Computed overdue check — NOT a stored status.
     * Returns {@code true} when PENDING and past dueDate.
     */
    public boolean isOverdue() {
        return this.status == InvoiceStatus.PENDING
                && this.dueDate != null
                && Instant.now().isAfter(this.dueDate);
    }

    // ─── equals / hashCode (ID-based) ─────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Invoice other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
