package com.lumie.billing.application.port.out;

import com.lumie.billing.domain.entity.BillingKey;

import java.util.List;
import java.util.Optional;

public interface BillingKeyPersistencePort {
    BillingKey save(BillingKey billingKey);
    Optional<BillingKey> findById(Long id);
    Optional<BillingKey> findActiveByTenantId(Long tenantId);
    List<BillingKey> findAllByTenantId(Long tenantId);
    boolean existsActiveByTenantId(Long tenantId);
}
