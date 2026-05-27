package com.lumie.tuition.adapter.out.persistence;

import com.lumie.tuition.domain.entity.Guardian;
import com.lumie.tuition.domain.repository.GuardianRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GuardianJpaRepository
        extends GuardianRepository, JpaRepository<Guardian, Long> {
}
