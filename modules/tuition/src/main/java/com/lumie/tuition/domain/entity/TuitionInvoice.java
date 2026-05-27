package com.lumie.tuition.domain.entity;

import com.lumie.tuition.domain.vo.Money;
import com.lumie.common.domain.TenantScopedEntity;
import com.lumie.tuition.domain.exception.TuitionErrorCode;
import com.lumie.tuition.domain.exception.TuitionException;
import com.lumie.tuition.domain.vo.LineItem;
import com.lumie.tuition.domain.vo.TuitionInvoiceStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "tuition_invoices", indexes = {
        @Index(name = "idx_tuition_invoices_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_tuition_invoices_student_id", columnList = "student_id"),
        @Index(name = "idx_tuition_invoices_guardian_id", columnList = "guardian_id"),
        @Index(name = "idx_tuition_invoices_status", columnList = "status"),
        @Index(name = "idx_tuition_invoices_due_date", columnList = "due_date"),
        @Index(name = "idx_tuition_invoices_idempotency_key", columnList = "idempotency_key", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TuitionInvoice extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "guardian_id")
    private Long guardianId;

    @Column(name = "class_enrollment_id")
    private Long classEnrollmentId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "items", nullable = false, columnDefinition = "jsonb")
    private List<LineItem> items;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "total_amount", nullable = false))
    private Money totalAmount;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "vat_amount", nullable = false))
    })
    private Money vatAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TuitionInvoiceStatus status;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "idempotency_key", length = 100, unique = true)
    private String idempotencyKey;

    @Builder
    private TuitionInvoice(Long studentId, Long guardianId, Long classEnrollmentId,
                            String title, List<LineItem> items,
                            Money totalAmount, Money vatAmount, String idempotencyKey) {
        this.studentId = studentId;
        this.guardianId = guardianId;
        this.classEnrollmentId = classEnrollmentId;
        this.title = title;
        this.items = items;
        this.totalAmount = totalAmount;
        this.vatAmount = vatAmount;
        this.idempotencyKey = idempotencyKey;
        this.status = TuitionInvoiceStatus.DRAFT;
    }

    public static TuitionInvoice create(Long studentId, Long guardianId, Long classEnrollmentId,
                                         String title, List<LineItem> items,
                                         Money totalAmount, Money vatAmount,
                                         String idempotencyKey) {
        return TuitionInvoice.builder()
                .studentId(studentId)
                .guardianId(guardianId)
                .classEnrollmentId(classEnrollmentId)
                .title(title)
                .items(items)
                .totalAmount(totalAmount)
                .vatAmount(vatAmount)
                .idempotencyKey(idempotencyKey)
                .build();
    }

    public void issue(Instant dueDate) {
        if (this.status != TuitionInvoiceStatus.DRAFT) {
            throw new TuitionException(TuitionErrorCode.INVALID_INVOICE_STATUS,
                    "Invoice can only be issued from DRAFT status");
        }
        this.status = TuitionInvoiceStatus.ISSUED;
        this.issuedAt = Instant.now();
        this.dueDate = dueDate;
    }

    public void markPaid(Instant paidAt) {
        if (this.status != TuitionInvoiceStatus.ISSUED && this.status != TuitionInvoiceStatus.OVERDUE) {
            throw new TuitionException(TuitionErrorCode.INVALID_INVOICE_STATUS,
                    "Invoice can only be marked as paid when ISSUED or OVERDUE");
        }
        this.status = TuitionInvoiceStatus.PAID;
        this.paidAt = paidAt;
    }

    public void markOverdue() {
        if (this.status != TuitionInvoiceStatus.ISSUED) {
            return;
        }
        this.status = TuitionInvoiceStatus.OVERDUE;
    }

    public void cancel(String reason) {
        if (this.status == TuitionInvoiceStatus.PAID) {
            throw new TuitionException(TuitionErrorCode.INVALID_INVOICE_STATUS,
                    "Paid invoices cannot be cancelled directly. Initiate a refund first.");
        }
        if (this.status == TuitionInvoiceStatus.CANCELLED) {
            throw new TuitionException(TuitionErrorCode.INVALID_INVOICE_STATUS,
                    "Invoice is already cancelled");
        }
        this.status = TuitionInvoiceStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.cancellationReason = reason;
    }

    public boolean isPaid() {
        return this.status == TuitionInvoiceStatus.PAID;
    }

    public boolean isIssuedOrOverdue() {
        return this.status == TuitionInvoiceStatus.ISSUED
                || this.status == TuitionInvoiceStatus.OVERDUE;
    }

    public boolean isOverdueByDate() {
        return (this.status == TuitionInvoiceStatus.ISSUED)
                && this.dueDate != null
                && Instant.now().isAfter(this.dueDate);
    }
}
