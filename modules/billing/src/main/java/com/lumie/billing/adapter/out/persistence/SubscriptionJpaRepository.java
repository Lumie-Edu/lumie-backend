package com.lumie.billing.adapter.out.persistence;

import com.lumie.billing.domain.entity.Subscription;
import com.lumie.billing.domain.vo.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SubscriptionJpaRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByTenantId(Long tenantId);

    Optional<Subscription> findByTenantSlug(String tenantSlug);

    @Query("SELECT s FROM Subscription s WHERE s.tenantSlug = :tenantSlug " +
           "AND s.status = com.lumie.billing.domain.vo.SubscriptionStatus.ACTIVE")
    Optional<Subscription> findActiveByTenantSlug(@Param("tenantSlug") String tenantSlug);

    List<Subscription> findAllByStatus(SubscriptionStatus status);

    @Query("SELECT s FROM Subscription s JOIN FETCH s.plan " +
           "WHERE s.status = com.lumie.billing.domain.vo.SubscriptionStatus.ACTIVE " +
           "AND s.nextChargeAt IS NOT NULL AND s.nextChargeAt <= :threshold")
    List<Subscription> findActiveWithNextChargeAtOrBefore(@Param("threshold") Instant threshold);

    boolean existsByTenantId(Long tenantId);
}
