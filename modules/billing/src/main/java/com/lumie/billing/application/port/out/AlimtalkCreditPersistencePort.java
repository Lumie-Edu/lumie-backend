package com.lumie.billing.application.port.out;

import com.lumie.billing.domain.entity.AlimtalkCredit;

import java.util.Optional;

public interface AlimtalkCreditPersistencePort {
    AlimtalkCredit save(AlimtalkCredit credit);
    Optional<AlimtalkCredit> findById(Long id);
    Optional<AlimtalkCredit> findByTenantId(Long tenantId);
}
