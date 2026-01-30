package com.lumie.billing.adapter.in.web;

import com.lumie.billing.application.dto.response.PlanResponse;
import com.lumie.billing.application.port.in.GetPlansUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class PlanController {

    private final GetPlansUseCase getPlansUseCase;

    @GetMapping
    public ResponseEntity<List<PlanResponse>> getAllPlans() {
        List<PlanResponse> plans = getPlansUseCase.getAllPlans();
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/{planId}")
    public ResponseEntity<PlanResponse> getPlanById(@PathVariable String planId) {
        PlanResponse plan = getPlansUseCase.getPlanById(planId);
        return ResponseEntity.ok(plan);
    }
}
