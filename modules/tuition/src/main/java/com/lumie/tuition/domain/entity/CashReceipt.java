package com.lumie.tuition.domain.entity;

import com.lumie.tuition.domain.vo.Money;
import com.lumie.common.domain.TenantScopedEntity;
import com.lumie.tuition.domain.exception.TuitionErrorCode;
import com.lumie.tuition.domain.exception.TuitionException;
import com.lumie.tuition.domain.vo.CashReceiptIssueStatus;
import com.lumie.tuition.domain.vo.CashReceiptType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "cash_receipts", indexes = {
        @Index(name = "idx_cash_receipts_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_cash_receipts_payment_id", columnList = "tuition_payment_id"),
        @Index(name = "idx_cash_receipts_issue_status", columnList = "issue_status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CashReceipt extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "tuition_payment_id")
    private Long tuitionPaymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "receipt_type", nullable = false, length = 20)
    private CashReceiptType receiptType;

    @Column(name = "identifier", nullable = false, length = 50)
    private String identifier;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "amount", nullable = false))
    private Money amount;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "vat_amount", nullable = false))
    })
    private Money vatAmount;

    @Column(name = "approval_number", length = 50)
    private String approvalNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_status", nullable = false, length = 20)
    private CashReceiptIssueStatus issueStatus;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "external_id", length = 200)
    private String externalId;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Builder
    private CashReceipt(Long tuitionPaymentId, CashReceiptType receiptType,
                         String identifier, Money amount, Money vatAmount) {
        this.tuitionPaymentId = tuitionPaymentId;
        this.receiptType = receiptType;
        this.identifier = identifier;
        this.amount = amount;
        this.vatAmount = vatAmount;
        this.issueStatus = CashReceiptIssueStatus.PENDING;
    }

    public static CashReceipt create(Long tuitionPaymentId, CashReceiptType receiptType,
                                      String identifier, Money amount, Money vatAmount) {
        return CashReceipt.builder()
                .tuitionPaymentId(tuitionPaymentId)
                .receiptType(receiptType)
                .identifier(identifier)
                .amount(amount)
                .vatAmount(vatAmount)
                .build();
    }

    public void markIssued(String approvalNumber, String externalId) {
        if (this.issueStatus != CashReceiptIssueStatus.PENDING) {
            throw new TuitionException(TuitionErrorCode.INVALID_CASH_RECEIPT_STATUS,
                    "Cash receipt can only be marked issued when PENDING");
        }
        this.approvalNumber = approvalNumber;
        this.externalId = externalId;
        this.issueStatus = CashReceiptIssueStatus.ISSUED;
        this.issuedAt = Instant.now();
    }

    public void markFailed(String reason) {
        if (this.issueStatus != CashReceiptIssueStatus.PENDING) {
            return;
        }
        this.issueStatus = CashReceiptIssueStatus.FAILED;
        this.failureReason = reason;
    }
}
