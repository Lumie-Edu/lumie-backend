package com.lumie.billing.domain.entity;

import com.lumie.billing.domain.exception.BillingErrorCode;
import com.lumie.billing.domain.exception.BillingException;
import com.lumie.billing.domain.vo.Money;
import com.lumie.billing.domain.vo.TaxInvoiceIssueStatus;
import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

/**
 * Korean tax invoice (세금계산서) issued via Popbill or similar SaaS.
 * Linked optionally to a billing Invoice.
 */
@Entity
@Table(name = "tax_invoices", indexes = {
        @Index(name = "idx_tax_invoices_invoice_id", columnList = "invoice_id"),
        @Index(name = "idx_tax_invoices_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_tax_invoices_issue_status", columnList = "issue_status"),
        @Index(name = "idx_tax_invoices_external_invoice_id", columnList = "external_invoice_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaxInvoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** Optional FK to billing invoices.id — null for alimtalk recharge tax invoices. */
    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "buyer_business_no", nullable = false, length = 20)
    private String buyerBusinessNo;

    @Column(name = "supplier_business_no", nullable = false, length = 20)
    private String supplierBusinessNo;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "amount", nullable = false))
    private Money amount;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "vat_amount", nullable = false))
    })
    private Money vatAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_status", nullable = false, length = 20)
    private TaxInvoiceIssueStatus issueStatus;

    @Column(name = "external_invoice_id", length = 100)
    private String externalInvoiceId;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Builder
    private TaxInvoice(Long invoiceId, Long tenantId, String buyerBusinessNo,
                       String supplierBusinessNo, Money amount, Money vatAmount) {
        this.invoiceId = invoiceId;
        this.tenantId = tenantId;
        this.buyerBusinessNo = buyerBusinessNo;
        this.supplierBusinessNo = supplierBusinessNo;
        this.amount = amount;
        this.vatAmount = vatAmount != null ? vatAmount : Money.zero();
        this.issueStatus = TaxInvoiceIssueStatus.PENDING;
    }

    public static TaxInvoice create(Long invoiceId, Long tenantId,
                                     String buyerBusinessNo, String supplierBusinessNo,
                                     Money amount, Money vatAmount) {
        return TaxInvoice.builder()
                .invoiceId(invoiceId)
                .tenantId(tenantId)
                .buyerBusinessNo(buyerBusinessNo)
                .supplierBusinessNo(supplierBusinessNo)
                .amount(amount)
                .vatAmount(vatAmount)
                .build();
    }

    // ─── Domain methods ───────────────────────────────────────────────────────

    public void markIssued(String externalId, Instant issuedAt) {
        if (this.issueStatus != TaxInvoiceIssueStatus.PENDING) {
            throw new BillingException(BillingErrorCode.TAX_INVOICE_ALREADY_ISSUED,
                    "Tax invoice is not in PENDING state. Current: " + this.issueStatus);
        }
        this.issueStatus = TaxInvoiceIssueStatus.ISSUED;
        this.externalInvoiceId = externalId;
        this.issuedAt = issuedAt != null ? issuedAt : Instant.now();
    }

    public void markFailed(String reason) {
        if (this.issueStatus != TaxInvoiceIssueStatus.PENDING) {
            return;
        }
        this.issueStatus = TaxInvoiceIssueStatus.FAILED;
        this.failureReason = reason;
    }

    // ─── Query methods ────────────────────────────────────────────────────────

    public boolean isIssued() {
        return this.issueStatus == TaxInvoiceIssueStatus.ISSUED;
    }

    // ─── equals / hashCode (ID-based) ─────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaxInvoice other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
