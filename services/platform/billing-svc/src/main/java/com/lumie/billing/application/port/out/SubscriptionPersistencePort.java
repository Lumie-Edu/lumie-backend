package com.lumie.billing.application.port.out;

import com.lumie.billing.domain.entity.Subscription;
import com.lumie.billing.domain.vo.SubscriptionStatus;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPersistencePort {
    Subscription save(Subscription subscription);
    Optional<Subscription> findById(Long id);
    Optional<Subscription> findByTenantId(Long tenantId);
    Optional<Subscription> findByTenantSlug(String tenantSlug);
    Optional<Subscription> findActiveByTenantSlug(String tenantSlug);
    List<Subscription> findAllByStatus(SubscriptionStatus status);
    boolean existsByTenantId(Long tenantId);
    void delete(Subscription subscription);
}
