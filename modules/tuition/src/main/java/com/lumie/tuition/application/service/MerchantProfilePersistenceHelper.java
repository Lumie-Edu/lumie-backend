package com.lumie.tuition.application.service;

import com.lumie.tuition.domain.entity.MerchantProfile;
import com.lumie.tuition.domain.repository.MerchantProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional helper for MerchantProfileCommandService.
 * Extracted to a separate bean so Spring proxy can apply @Transactional correctly
 * (self-invocation from MerchantProfileCommandService would bypass the proxy).
 */
@Service
@RequiredArgsConstructor
public class MerchantProfilePersistenceHelper {

    private final MerchantProfileRepository merchantProfileRepository;

    @Transactional
    public MerchantProfile saveMerchantProfile(MerchantProfile profile) {
        return merchantProfileRepository.save(profile);
    }

    @Transactional
    public void approveAndSaveMerchantProfile(Long profileId, String sellerKey) {
        merchantProfileRepository.findById(profileId).ifPresent(p -> {
            p.approve(sellerKey);
            merchantProfileRepository.save(p);
        });
    }
}
