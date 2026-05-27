package com.lumie.tuition.adapter.out.persistence;

import com.lumie.tuition.domain.entity.MerchantProfile;
import com.lumie.tuition.domain.repository.MerchantProfileRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MerchantProfileJpaRepository
        extends MerchantProfileRepository, JpaRepository<MerchantProfile, Long> {
}
