package com.lumie.billing.adapter.out.persistence;

import com.lumie.billing.application.port.out.PaymentTransactionPersistencePort;
import com.lumie.billing.domain.entity.PaymentTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PaymentTransactionPersistenceAdapter implements PaymentTransactionPersistencePort {

    private final PaymentTransactionJpaRepository paymentTransactionJpaRepository;

    @Override
    public PaymentTransaction save(PaymentTransaction transaction) {
        return paymentTransactionJpaRepository.save(transaction);
    }

    @Override
    public Optional<PaymentTransaction> findByTransactionId(String transactionId) {
        return paymentTransactionJpaRepository.findByTransactionId(transactionId);
    }

    @Override
    public List<PaymentTransaction> findByOwner(String ownerEntity, Long ownerId) {
        return paymentTransactionJpaRepository.findByOwnerEntityAndOwnerId(ownerEntity, ownerId);
    }

    @Override
    public List<PaymentTransaction> findByTenantId(Long tenantId) {
        return paymentTransactionJpaRepository.findByTenantId(tenantId);
    }
}
