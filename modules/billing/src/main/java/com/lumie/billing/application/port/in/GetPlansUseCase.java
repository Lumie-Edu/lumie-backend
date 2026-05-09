package com.lumie.billing.application.port.in;

import com.lumie.billing.application.dto.response.PlanResponse;

import java.util.List;

public interface GetPlansUseCase {
    List<PlanResponse> getAllPlans();
    PlanResponse getPlanById(String planId);
}
