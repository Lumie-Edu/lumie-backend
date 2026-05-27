package com.lumie.billing.application.service;

import com.lumie.billing.application.dto.response.BillingKeyResponse;
import com.lumie.billing.application.port.out.BillingKeyPersistencePort;
import com.lumie.billing.domain.exception.BillingErrorCode;
import com.lumie.billing.domain.exception.BillingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillingKeyQueryService {

    private final BillingKeyPersistencePort billingKeyPersistencePort;

    public BillingKeyResponse getActiveBillingKey(Long tenantId) {
        return billingKeyPersistencePort.findActiveByTenantId(tenantId)
                .map(BillingKeyResponse::from)
                .orElseThrow(() -> new BillingException(BillingErrorCode.BILLING_KEY_NOT_FOUND,
                        "No active billing key for tenant: " + tenantId));
    }

    public List<BillingKeyResponse> getAllBillingKeys(Long tenantId) {
        return billingKeyPersistencePort.findAllByTenantId(tenantId).stream()
                .map(BillingKeyResponse::from)
                .toList();
    }
}
