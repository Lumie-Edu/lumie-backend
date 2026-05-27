package com.lumie.tuition.domain.repository;

import com.lumie.tuition.domain.entity.MerchantProfile;
import com.lumie.tuition.domain.vo.KycStatus;

import java.util.Optional;

public interface MerchantProfileRepository {

    MerchantProfile save(MerchantProfile profile);

    Optional<MerchantProfile> findById(Long id);

    /**
     * Each tenant has at most one merchant profile (enforced by unique index on tenant_id).
     * RLS ensures this returns only the current tenant's profile.
     */
    Optional<MerchantProfile> findFirstBy();

    Optional<MerchantProfile> findByKycStatus(KycStatus kycStatus);

    boolean existsByKycStatus(KycStatus kycStatus);
}
