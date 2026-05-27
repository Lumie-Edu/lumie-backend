package com.lumie.billing.adapter.out.persistence;

import com.lumie.billing.application.port.out.AlimtalkCreditPersistencePort;
import com.lumie.billing.domain.entity.AlimtalkCredit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AlimtalkCreditPersistenceAdapter implements AlimtalkCreditPersistencePort {

    private final AlimtalkCreditJpaRepository alimtalkCreditJpaRepository;

    @Override
    public AlimtalkCredit save(AlimtalkCredit credit) {
        return alimtalkCreditJpaRepository.save(credit);
    }

    @Override
    public Optional<AlimtalkCredit> findById(Long id) {
        return alimtalkCreditJpaRepository.findById(id);
    }

    @Override
    public Optional<AlimtalkCredit> findByTenantId(Long tenantId) {
        return alimtalkCreditJpaRepository.findByTenantId(tenantId);
    }
}
