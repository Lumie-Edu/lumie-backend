package com.lumie.billing.application.port.out;

import com.lumie.billing.domain.entity.BillingKey;

import java.util.List;
import java.util.Optional;

public interface BillingKeyPersistencePort {
    BillingKey save(BillingKey billingKey);

    /**
     * Same as {@link #save(BillingKey)} but forces the SQL statement to be
     * flushed to the database immediately rather than at transaction commit.
     *
     * <p>Needed for the
     * {@code BillingKeyPersistenceHelper.replaceActive} flow: the existing
     * ACTIVE row must be {@code UPDATE}d to REVOKED before the new ACTIVE
     * row's {@code INSERT} fires, otherwise the partial unique index
     * {@code uq_billing_keys_tenant_active} sees two ACTIVE rows in the
     * same transaction and rejects the INSERT (23505).
     */
    BillingKey saveAndFlush(BillingKey billingKey);

    Optional<BillingKey> findById(Long id);
    Optional<BillingKey> findActiveByTenantId(Long tenantId);
    List<BillingKey> findAllByTenantId(Long tenantId);
    boolean existsActiveByTenantId(Long tenantId);
}
