package com.lumie.billing.domain.repository;

import com.lumie.billing.domain.entity.Plan;

import java.util.List;
import java.util.Optional;

public interface PlanRepository {
    Plan save(Plan plan);
    Optional<Plan> findById(String id);
    List<Plan> findAllActive();
    List<Plan> findAll();
    boolean existsById(String id);
}
