package com.lumie.billing.adapter.out.persistence;

import com.lumie.billing.application.port.out.PlanPersistencePort;
import com.lumie.billing.domain.entity.Plan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PlanPersistenceAdapter implements PlanPersistencePort {

    private final PlanJpaRepository planJpaRepository;

    @Override
    public Plan save(Plan plan) {
        return planJpaRepository.save(plan);
    }

    @Override
    public Optional<Plan> findById(String id) {
        return planJpaRepository.findById(id);
    }

    @Override
    public List<Plan> findAllActive() {
        return planJpaRepository.findAllActive();
    }

    @Override
    public List<Plan> findAll() {
        return planJpaRepository.findAllOrderByDisplayOrder();
    }

    @Override
    public boolean existsById(String id) {
        return planJpaRepository.existsById(id);
    }
}
