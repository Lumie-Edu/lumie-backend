package com.lumie.billing.domain.entity;

import com.lumie.billing.domain.exception.PaymentFailedException;
import com.lumie.billing.domain.vo.InvoiceStatus;
import com.lumie.billing.domain.vo.Money;
import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "invoices", indexes = {
        @Index(name = "idx_invoices_subscription_id", columnList = "subscription_id"),
        @Index(name = "idx_invoices_tenant_slug", columnList = "tenant_slug"),
        @Index(name = "idx_invoices_status", columnList = "status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "tenant_slug", nullable = false, length = 30)
    private String tenantSlug;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "amount", nullable = false))
    private Money amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InvoiceStatus status;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "billing_period_start")
    private LocalDateTime billingPeriodStart;

    @Column(name = "billing_period_end")
    private LocalDateTime billingPeriodEnd;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "payment_key", length = 200)
    private String paymentKey;

    @Column(name = "order_id", length = 100)
    private String orderId;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Builder
    private Invoice(String invoiceNumber, Subscription subscription, String tenantSlug,
                    Money amount, String description, LocalDateTime billingPeriodStart,
                    LocalDateTime billingPeriodEnd, LocalDateTime dueDate, String orderId) {
        this.invoiceNumber = invoiceNumber;
        this.subscription = subscription;
        this.tenantSlug = tenantSlug;
        this.amount = amount;
        this.status = InvoiceStatus.PENDING;
        this.description = description;
        this.billingPeriodStart = billingPeriodStart;
        this.billingPeriodEnd = billingPeriodEnd;
        this.dueDate = dueDate;
        this.orderId = orderId;
    }

    public static Invoice create(String invoiceNumber, Subscription subscription, Money amount,
                                  String description, LocalDateTime billingPeriodStart,
                                  LocalDateTime billingPeriodEnd, LocalDateTime dueDate, String orderId) {
        return Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .subscription(subscription)
                .tenantSlug(subscription.getTenantSlug())
                .amount(amount)
                .description(description)
                .billingPeriodStart(billingPeriodStart)
                .billingPeriodEnd(billingPeriodEnd)
                .dueDate(dueDate)
                .orderId(orderId)
                .build();
    }

    public void markAsPaid(String paymentKey) {
        if (this.status != InvoiceStatus.PENDING) {
            throw new PaymentFailedException("Invoice is not in pending status");
        }
        this.status = InvoiceStatus.PAID;
        this.paidAt = LocalDateTime.now();
        this.paymentKey = paymentKey;
    }

    public void markAsFailed(String reason) {
        if (this.status != InvoiceStatus.PENDING) {
            return;
        }
        this.status = InvoiceStatus.FAILED;
        this.failureReason = reason;
    }

    public void refund() {
        if (this.status != InvoiceStatus.PAID) {
            throw new PaymentFailedException("Only paid invoices can be refunded");
        }
        this.status = InvoiceStatus.REFUNDED;
    }

    public boolean isPaid() {
        return this.status == InvoiceStatus.PAID;
    }

    public boolean isPending() {
        return this.status == InvoiceStatus.PENDING;
    }

    public boolean isOverdue() {
        return this.status == InvoiceStatus.PENDING
                && this.dueDate != null
                && LocalDateTime.now().isAfter(this.dueDate);
    }
}
