package com.lumie.billing.application.service;

import com.lumie.billing.application.port.out.BillingKeyPersistencePort;
import com.lumie.billing.domain.entity.BillingKey;
import com.lumie.billing.domain.exception.BillingErrorCode;
import com.lumie.billing.domain.exception.BillingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional helper for billing key persistence.
 */
@Service
@RequiredArgsConstructor
public class BillingKeyPersistenceHelper {

    private final BillingKeyPersistencePort billingKeyPersistencePort;

    @Transactional
    public BillingKey save(BillingKey billingKey) {
        return billingKeyPersistencePort.save(billingKey);
    }

    @Transactional
    public void revoke(Long billingKeyId) {
        BillingKey billingKey = billingKeyPersistencePort.findById(billingKeyId)
                .orElseThrow(() -> new BillingException(BillingErrorCode.BILLING_KEY_NOT_FOUND,
                        "BillingKey not found: " + billingKeyId));
        billingKey.revoke();
        billingKeyPersistencePort.save(billingKey);
    }

    /**
     * Atomically replace the tenant's active billing key with a new one.
     *
     * <p>The partial unique index {@code uq_billing_keys_tenant_active} permits
     * a single ACTIVE row per tenant; revoking the existing row inside the same
     * transaction frees the slot for the new INSERT. Re-registration (user
     * retry, card swap, first-time setup) all share this semantics, which
     * matches the BE invariant "one active card per tenant".
     */
    @Transactional
    public BillingKey replaceActive(BillingKey newBillingKey) {
        billingKeyPersistencePort.findActiveByTenantId(newBillingKey.getTenantId())
                .ifPresent(existing -> {
                    existing.revoke();
                    billingKeyPersistencePort.save(existing);
                });
        return billingKeyPersistencePort.save(newBillingKey);
    }
}
