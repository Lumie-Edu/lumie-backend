package com.lumie.billing.application.port.out;

import com.lumie.billing.domain.entity.Plan;

import java.util.List;
import java.util.Optional;

public interface PlanPersistencePort {
    Plan save(Plan plan);
    Optional<Plan> findById(String id);
    List<Plan> findAllActive();
    List<Plan> findAll();
    boolean existsById(String id);
}
