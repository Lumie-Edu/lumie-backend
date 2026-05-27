package com.lumie.tuition.application.service;

import com.lumie.tuition.application.dto.response.MerchantProfileResponse;
import com.lumie.tuition.domain.exception.TuitionErrorCode;
import com.lumie.tuition.domain.exception.TuitionException;
import com.lumie.tuition.domain.repository.MerchantProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MerchantProfileQueryService {

    private final MerchantProfileRepository merchantProfileRepository;

    public MerchantProfileResponse getMyProfile() {
        return merchantProfileRepository.findFirstBy()
                .map(MerchantProfileResponse::from)
                .orElseThrow(() -> new TuitionException(TuitionErrorCode.MERCHANT_PROFILE_NOT_FOUND,
                        "No merchant profile found for this tenant"));
    }

    public MerchantProfileResponse getById(Long id) {
        return merchantProfileRepository.findById(id)
                .map(MerchantProfileResponse::from)
                .orElseThrow(() -> new TuitionException(TuitionErrorCode.MERCHANT_PROFILE_NOT_FOUND,
                        "MerchantProfile not found: " + id));
    }
}
