package com.lumie.billing.adapter.out.persistence;

import com.lumie.billing.application.port.out.SubscriptionPersistencePort;
import com.lumie.billing.domain.entity.Subscription;
import com.lumie.billing.domain.vo.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SubscriptionPersistenceAdapter implements SubscriptionPersistencePort {

    private final SubscriptionJpaRepository subscriptionJpaRepository;

    @Override
    public Subscription save(Subscription subscription) {
        return subscriptionJpaRepository.save(subscription);
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
    public boolean existsByTenantId(Long tenantId) {
        return subscriptionJpaRepository.existsByTenantId(tenantId);
    }

    @Override
    public void delete(Subscription subscription) {
        subscriptionJpaRepository.delete(subscription);
    }
}
