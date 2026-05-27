package com.lumie.billing.application.service;

import com.lumie.billing.application.port.out.AlimtalkCreditPersistencePort;
import com.lumie.billing.domain.entity.AlimtalkCredit;
import com.lumie.billing.domain.exception.BillingErrorCode;
import com.lumie.billing.domain.exception.BillingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional helper for alimtalk credit persistence.
 */
@Service
@RequiredArgsConstructor
public class AlimtalkCreditPersistenceHelper {

    private final AlimtalkCreditPersistencePort alimtalkCreditPersistencePort;

    @Transactional
    public AlimtalkCredit save(AlimtalkCredit credit) {
        return alimtalkCreditPersistencePort.save(credit);
    }

    @Transactional
    public AlimtalkCredit consume(Long tenantId, int count) {
        AlimtalkCredit credit = alimtalkCreditPersistencePort.findByTenantId(tenantId)
                .orElseThrow(() -> new BillingException(BillingErrorCode.ALIMTALK_CREDIT_NOT_FOUND,
                        "AlimtalkCredit not found for tenant: " + tenantId));
        credit.consume(count);
        return alimtalkCreditPersistencePort.save(credit);
    }

    @Transactional
    public AlimtalkCredit recharge(Long tenantId, int packageSize) {
        AlimtalkCredit credit = alimtalkCreditPersistencePort.findByTenantId(tenantId)
                .orElseThrow(() -> new BillingException(BillingErrorCode.ALIMTALK_CREDIT_NOT_FOUND,
                        "AlimtalkCredit not found for tenant: " + tenantId));
        credit.recharge(packageSize);
        return alimtalkCreditPersistencePort.save(credit);
    }
}
