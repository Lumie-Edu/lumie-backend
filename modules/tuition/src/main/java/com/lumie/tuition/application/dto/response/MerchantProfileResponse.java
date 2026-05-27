package com.lumie.tuition.application.dto.response;

import com.lumie.tuition.domain.entity.MerchantProfile;
import com.lumie.tuition.domain.vo.KycStatus;

import java.time.Instant;

public record MerchantProfileResponse(
        Long id,
        String businessRegNumber,
        String representativeName,
        String settlementBankCode,
        String settlementAccountHolder,
        KycStatus kycStatus,
        Instant kycSubmittedAt,
        Instant kycCompletedAt,
        String rejectionReason,
        Instant createdAt,
        Instant updatedAt
) {
    public static MerchantProfileResponse from(MerchantProfile profile) {
        return new MerchantProfileResponse(
                profile.getId(),
                profile.getBusinessRegNumber(),
                profile.getRepresentativeName(),
                profile.getSettlementBankCode(),
                profile.getSettlementAccountHolder(),
                profile.getKycStatus(),
                profile.getKycSubmittedAt(),
                profile.getKycCompletedAt(),
                profile.getRejectionReason(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
