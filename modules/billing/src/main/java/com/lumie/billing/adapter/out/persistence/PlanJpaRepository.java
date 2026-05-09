package com.lumie.billing.adapter.out.persistence;

import com.lumie.billing.domain.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PlanJpaRepository extends JpaRepository<Plan, String> {

    @Query("SELECT p FROM Plan p WHERE p.active = true ORDER BY p.displayOrder")
    List<Plan> findAllActive();

    @Query("SELECT p FROM Plan p ORDER BY p.displayOrder")
    List<Plan> findAllOrderByDisplayOrder();
}
