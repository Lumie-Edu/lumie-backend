package com.lumie.billing.application.service;

import com.lumie.billing.application.dto.response.PlanResponse;
import com.lumie.billing.application.port.in.GetPlansUseCase;
import com.lumie.billing.application.port.out.PlanPersistencePort;
import com.lumie.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanQueryService implements GetPlansUseCase {

    private final PlanPersistencePort planPersistencePort;

    @Override
    public List<PlanResponse> getAllPlans() {
        log.debug("Fetching all active plans");
        return planPersistencePort.findAllActive().stream()
                .map(PlanResponse::from)
                .toList();
    }

    @Override
    public PlanResponse getPlanById(String planId) {
        log.debug("Fetching plan by id: {}", planId);
        return planPersistencePort.findById(planId)
                .map(PlanResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", planId));
    }
}
