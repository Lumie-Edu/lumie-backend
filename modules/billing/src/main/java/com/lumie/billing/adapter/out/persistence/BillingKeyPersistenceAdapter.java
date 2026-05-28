package com.lumie.billing.adapter.out.persistence;

import com.lumie.billing.application.port.out.BillingKeyPersistencePort;
import com.lumie.billing.domain.entity.BillingKey;
import com.lumie.billing.domain.vo.BillingKeyStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BillingKeyPersistenceAdapter implements BillingKeyPersistencePort {

    private final BillingKeyJpaRepository billingKeyJpaRepository;

    @Override
    public BillingKey save(BillingKey billingKey) {
        return billingKeyJpaRepository.save(billingKey);
    }

    @Override
    public BillingKey saveAndFlush(BillingKey billingKey) {
        return billingKeyJpaRepository.saveAndFlush(billingKey);
    }

    @Override
    public Optional<BillingKey> findById(Long id) {
        return billingKeyJpaRepository.findById(id);
    }

    @Override
    public Optional<BillingKey> findActiveByTenantId(Long tenantId) {
        return billingKeyJpaRepository.findActiveByTenantId(tenantId);
    }

    @Override
    public List<BillingKey> findAllByTenantId(Long tenantId) {
        return billingKeyJpaRepository.findAllByTenantId(tenantId);
    }

    @Override
    public boolean existsActiveByTenantId(Long tenantId) {
        return billingKeyJpaRepository.existsByTenantIdAndStatus(tenantId, BillingKeyStatus.ACTIVE);
    }
}
