package com.lumie.tuition.domain.entity;

import com.lumie.common.domain.TenantScopedEntity;
import com.lumie.tuition.domain.exception.TuitionErrorCode;
import com.lumie.tuition.domain.exception.TuitionException;
import com.lumie.tuition.domain.vo.KycStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "merchant_profiles", indexes = {
        @Index(name = "idx_merchant_profiles_tenant_id", columnList = "tenant_id", unique = true),
        @Index(name = "idx_merchant_profiles_kyc_status", columnList = "kyc_status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MerchantProfile extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "pg_seller_key", length = 200)
    private String pgSellerKey;

    @Column(name = "business_reg_number", nullable = false, length = 20)
    private String businessRegNumber;

    @Column(name = "representative_name", nullable = false, length = 100)
    private String representativeName;

    @Column(name = "settlement_bank_code", nullable = false, length = 10)
    private String settlementBankCode;

    @Column(name = "settlement_account_number", nullable = false, length = 50)
    private String settlementAccountNumber;

    @Column(name = "settlement_account_holder", nullable = false, length = 100)
    private String settlementAccountHolder;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 20)
    private KycStatus kycStatus;

    @Column(name = "kyc_submitted_at")
    private Instant kycSubmittedAt;

    @Column(name = "kyc_completed_at")
    private Instant kycCompletedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Builder
    private MerchantProfile(String businessRegNumber, String representativeName,
                             String settlementBankCode, String settlementAccountNumber,
                             String settlementAccountHolder) {
        this.businessRegNumber = businessRegNumber;
        this.representativeName = representativeName;
        this.settlementBankCode = settlementBankCode;
        this.settlementAccountNumber = settlementAccountNumber;
        this.settlementAccountHolder = settlementAccountHolder;
        this.kycStatus = KycStatus.PENDING;
    }

    public static MerchantProfile create(String businessRegNumber, String representativeName,
                                          String settlementBankCode, String settlementAccountNumber,
                                          String settlementAccountHolder) {
        return MerchantProfile.builder()
                .businessRegNumber(businessRegNumber)
                .representativeName(representativeName)
                .settlementBankCode(settlementBankCode)
                .settlementAccountNumber(settlementAccountNumber)
                .settlementAccountHolder(settlementAccountHolder)
                .build();
    }

    /**
     * Initial KYC submission — only valid from a newly-created profile (PENDING initial state).
     * For re-submission after rejection, use {@link #resubmit()}.
     */
    public void submit() {
        if (this.kycStatus != KycStatus.PENDING) {
            throw new TuitionException(TuitionErrorCode.INVALID_KYC_STATUS,
                    "KYC initial submission is only valid when status is PENDING. Use resubmit() after rejection.");
        }
        this.kycSubmittedAt = Instant.now();
        this.rejectionReason = null;
    }

    /**
     * Re-submission after rejection. Transitions REJECTED → PENDING.
     */
    public void resubmit() {
        if (this.kycStatus != KycStatus.REJECTED) {
            throw new TuitionException(TuitionErrorCode.INVALID_KYC_STATUS,
                    "KYC resubmission is only valid when status is REJECTED");
        }
        this.kycStatus = KycStatus.PENDING;
        this.kycSubmittedAt = Instant.now();
        this.rejectionReason = null;
    }

    public void approve(String sellerKey) {
        if (this.kycStatus != KycStatus.PENDING) {
            throw new TuitionException(TuitionErrorCode.INVALID_KYC_STATUS,
                    "KYC can only be approved when status is PENDING");
        }
        this.pgSellerKey = sellerKey;
        this.kycStatus = KycStatus.APPROVED;
        this.kycCompletedAt = Instant.now();
    }

    public void reject(String reason) {
        if (this.kycStatus != KycStatus.PENDING) {
            throw new TuitionException(TuitionErrorCode.INVALID_KYC_STATUS,
                    "KYC can only be rejected when status is PENDING");
        }
        this.kycStatus = KycStatus.REJECTED;
        this.kycCompletedAt = Instant.now();
        this.rejectionReason = reason;
    }

    public boolean isApproved() {
        return this.kycStatus == KycStatus.APPROVED;
    }
}
