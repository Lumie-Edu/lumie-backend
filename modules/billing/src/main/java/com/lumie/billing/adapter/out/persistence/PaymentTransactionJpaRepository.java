package com.lumie.billing.adapter.out.persistence;

import com.lumie.billing.domain.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionJpaRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByTransactionId(String transactionId);

    @Query("SELECT pt FROM PaymentTransaction pt " +
           "WHERE pt.ownerEntity = :ownerEntity AND pt.ownerId = :ownerId " +
           "ORDER BY pt.createdAt DESC")
    List<PaymentTransaction> findByOwnerEntityAndOwnerId(@Param("ownerEntity") String ownerEntity,
                                                          @Param("ownerId") Long ownerId);

    List<PaymentTransaction> findByTenantId(Long tenantId);
}
