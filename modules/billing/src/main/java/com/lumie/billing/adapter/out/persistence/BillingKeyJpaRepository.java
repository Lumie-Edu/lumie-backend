package com.lumie.billing.adapter.out.persistence;

import com.lumie.billing.domain.entity.BillingKey;
import com.lumie.billing.domain.vo.BillingKeyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BillingKeyJpaRepository extends JpaRepository<BillingKey, Long> {

    @Query("SELECT bk FROM BillingKey bk WHERE bk.tenantId = :tenantId " +
           "AND bk.status = com.lumie.billing.domain.vo.BillingKeyStatus.ACTIVE")
    Optional<BillingKey> findActiveByTenantId(@Param("tenantId") Long tenantId);

    List<BillingKey> findAllByTenantId(Long tenantId);

    boolean existsByTenantIdAndStatus(Long tenantId, BillingKeyStatus status);
}
