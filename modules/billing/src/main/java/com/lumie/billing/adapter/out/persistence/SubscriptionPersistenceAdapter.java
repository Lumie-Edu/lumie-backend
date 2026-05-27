package com.lumie.billing.adapter.out.persistence;

import com.lumie.billing.application.port.out.SubscriptionPersistencePort;
import com.lumie.billing.domain.entity.Subscription;
import com.lumie.billing.domain.vo.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class SubscriptionPersistenceAdapter implements SubscriptionPersistencePort {

    private final SubscriptionJpaRepository subscriptionJpaRepository;

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Subscription save(Subscription subscription) {
        return subscriptionJpaRepository.save(subscription);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void delete(Subscription subscription) {
        subscriptionJpaRepository.delete(subscription);
    }

    @Override
    public Optional<Subscription> findById(Long id) {
        return subscriptionJpaRepository.findById(id);
    }

    @Override
    public Optional<Subscription> findByTenantId(Long tenantId) {
        return subscriptionJpaRepository.findByTenantId(tenantId);
    }

    @Override
    public Optional<Subscription> findByTenantSlug(String tenantSlug) {
        return subscriptionJpaRepository.findByTenantSlug(tenantSlug);
    }

    @Override
    public Optional<Subscription> findActiveByTenantSlug(String tenantSlug) {
        return subscriptionJpaRepository.findActiveByTenantSlug(tenantSlug);
    }

    @Override
    public List<Subscription> findAllByStatus(SubscriptionStatus status) {
        return subscriptionJpaRepository.findAllByStatus(status);
    }

    @Override
    public List<Subscription> findActiveWithNextChargeAtOrBefore(Instant threshold) {
        return subscriptionJpaRepository.findActiveWithNextChargeAtOrBefore(threshold);
    }

    @Override
    public boolean existsByTenantId(Long tenantId) {
        return subscriptionJpaRepository.existsByTenantId(tenantId);
    }
}
