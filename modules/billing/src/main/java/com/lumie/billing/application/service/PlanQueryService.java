package com.lumie.billing.application.service;

import com.lumie.billing.application.dto.response.PlanResponse;
import com.lumie.billing.application.port.out.PlanPersistencePort;
import com.lumie.common.cache.CacheConfig;
import com.lumie.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read-only query service for the Plan catalogue.
 *
 * <p><b>Cache assumption</b>: plans are Flyway-seeded ({@code public.plans}
 * table populated by V1) and not mutated at runtime — there is no Plan
 * CRUD admin path. Therefore {@link CacheConfig#CACHE_PLANS} has no
 * {@code @CacheEvict} sites; entries simply expire after 1 h.
 *
 * <p>If a future change introduces runtime plan mutation (e.g., a tier
 * pricing tweak via admin UI), add {@code @CacheEvict(value = CACHE_PLANS,
 * allEntries = true)} on the mutator and remove this comment.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanQueryService {

    private final PlanPersistencePort planPersistencePort;

    @Cacheable(value = CacheConfig.CACHE_PLANS, key = "'all'")
    public List<PlanResponse> getAllPlans() {
        log.debug("Fetching all active plans");
        return planPersistencePort.findAllActive().stream()
                .map(PlanResponse::from)
                .toList();
    }

    @Cacheable(value = CacheConfig.CACHE_PLANS, key = "#planId")
    public PlanResponse getPlanById(String planId) {
        log.debug("Fetching plan by id: {}", planId);
        return planPersistencePort.findById(planId)
                .map(PlanResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", planId));
    }
}
