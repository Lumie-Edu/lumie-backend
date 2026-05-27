package com.lumie.billing.application.service;

import com.lumie.billing.application.dto.response.AlimtalkCreditResponse;
import com.lumie.billing.application.port.out.AlimtalkCreditPersistencePort;
import com.lumie.billing.domain.exception.BillingErrorCode;
import com.lumie.billing.domain.exception.BillingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlimtalkCreditQueryService {

    private final AlimtalkCreditPersistencePort alimtalkCreditPersistencePort;

    public AlimtalkCreditResponse getCreditBalance(Long tenantId) {
        return alimtalkCreditPersistencePort.findByTenantId(tenantId)
                .map(AlimtalkCreditResponse::from)
                .orElseThrow(() -> new BillingException(BillingErrorCode.ALIMTALK_CREDIT_NOT_FOUND,
                        "AlimtalkCredit account not found for tenant: " + tenantId));
    }
}
