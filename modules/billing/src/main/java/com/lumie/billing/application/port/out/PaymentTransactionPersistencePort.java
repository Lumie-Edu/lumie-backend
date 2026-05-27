package com.lumie.billing.application.port.out;

import com.lumie.billing.domain.entity.PaymentTransaction;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionPersistencePort {
    PaymentTransaction save(PaymentTransaction transaction);
    Optional<PaymentTransaction> findByTransactionId(String transactionId);
    List<PaymentTransaction> findByOwner(String ownerEntity, Long ownerId);
    List<PaymentTransaction> findByTenantId(Long tenantId);
}
