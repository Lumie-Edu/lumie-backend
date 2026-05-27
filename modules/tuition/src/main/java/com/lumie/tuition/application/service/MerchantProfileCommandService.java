package com.lumie.tuition.application.service;

import com.lumie.tuition.application.dto.request.SubmitKycRequest;
import com.lumie.tuition.application.dto.response.MerchantProfileResponse;
import com.lumie.tuition.application.port.out.TuitionPaymentGatewayPort;
import com.lumie.tuition.domain.entity.MerchantProfile;
import com.lumie.tuition.domain.exception.TuitionErrorCode;
import com.lumie.tuition.domain.exception.TuitionException;
import com.lumie.tuition.domain.repository.MerchantProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantProfileCommandService {

    private final MerchantProfileRepository merchantProfileRepository;
    private final TuitionPaymentGatewayPort paymentGatewayPort;
    private final MerchantProfilePersistenceHelper persistenceHelper;

    /**
     * Creates (or re-submits) a merchant profile and submits KYC documents to Toss.
     * PG call is outside @Transactional per HARD_RULES.
     */
    public MerchantProfileResponse submitKyc(SubmitKycRequest request) {
        boolean isNew = merchantProfileRepository.findFirstBy().isEmpty();
        MerchantProfile profile = merchantProfileRepository.findFirstBy()
                .orElseGet(() -> MerchantProfile.create(
                        request.businessRegNumber(),
                        request.representativeName(),
                        request.settlementBankCode(),
                        request.settlementAccountNumber(),
                        request.settlementAccountHolder()
                ));

        // New profiles start as PENDING and use submit(); existing rejected ones use resubmit()
        if (isNew) {
            profile.submit();
        } else {
            profile.resubmit();
        }
        // Persist via helper bean so @Transactional proxy applies (separate bean = no self-invocation)
        MerchantProfile saved = persistenceHelper.saveMerchantProfile(profile);

        // PG call outside @Transactional
        TuitionPaymentGatewayPort.KycDocuments docs = new TuitionPaymentGatewayPort.KycDocuments(
                request.businessRegistrationDocUrl(),
                request.bankbookDocUrl()
        );
        TuitionPaymentGatewayPort.SellerRegistrationResult pgResult =
                paymentGatewayPort.registerSeller(saved, docs);

        if (pgResult.success()) {
            persistenceHelper.approveAndSaveMerchantProfile(saved.getId(), pgResult.pgSellerKey());
            log.info("MerchantProfile KYC approved via PG for id={}", saved.getId());
        } else {
            log.warn("MerchantProfile KYC submission accepted, awaiting PG confirmation: id={}", saved.getId());
        }

        return MerchantProfileResponse.from(merchantProfileRepository.findById(saved.getId()).orElseThrow());
    }

    @Transactional
    public MerchantProfileResponse approveKyc(Long profileId, String sellerKey) {
        MerchantProfile profile = merchantProfileRepository.findById(profileId)
                .orElseThrow(() -> new TuitionException(TuitionErrorCode.MERCHANT_PROFILE_NOT_FOUND,
                        "MerchantProfile not found: " + profileId));
        profile.approve(sellerKey);
        MerchantProfile saved = merchantProfileRepository.save(profile);
        log.info("MerchantProfile KYC approved: id={}", profileId);
        return MerchantProfileResponse.from(saved);
    }

    @Transactional
    public MerchantProfileResponse rejectKyc(Long profileId, String reason) {
        MerchantProfile profile = merchantProfileRepository.findById(profileId)
                .orElseThrow(() -> new TuitionException(TuitionErrorCode.MERCHANT_PROFILE_NOT_FOUND,
                        "MerchantProfile not found: " + profileId));
        profile.reject(reason);
        MerchantProfile saved = merchantProfileRepository.save(profile);
        log.info("MerchantProfile KYC rejected: id={}, reason={}", profileId, reason);
        return MerchantProfileResponse.from(saved);
    }
}
