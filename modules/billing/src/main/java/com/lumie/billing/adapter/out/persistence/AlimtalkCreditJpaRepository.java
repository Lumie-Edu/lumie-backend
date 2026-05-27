package com.lumie.billing.adapter.out.persistence;

import com.lumie.billing.domain.entity.AlimtalkCredit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlimtalkCreditJpaRepository extends JpaRepository<AlimtalkCredit, Long> {

    Optional<AlimtalkCredit> findByTenantId(Long tenantId);
}
