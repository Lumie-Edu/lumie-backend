package com.lumie.billing.application.service;

import com.lumie.billing.application.port.out.PlanPersistencePort;
import com.lumie.billing.infrastructure.config.PaddleConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "paddle.enabled", havingValue = "true")
@Transactional(readOnly = true)
public class CheckoutSettingsService {

    public record CheckoutSettings(String clientToken, String environment, Map<String, PlanPriceIds> plans) {}

    public record PlanPriceIds(String monthlyPriceId, String yearlyPriceId) {}

    private final PaddleConfig paddleConfig;
    private final PlanPersistencePort planPersistencePort;

    public CheckoutSettings getCheckoutSettings() {
        Map<String, PlanPriceIds> plans = new LinkedHashMap<>();
        for (var plan : planPersistencePort.findAllActive()) {
            if (plan.isFree()) continue;
            plans.put(plan.getId(), new PlanPriceIds(
                    plan.getPaddleMonthlyPriceId(),
                    plan.getPaddleYearlyPriceId()));
        }
        return new CheckoutSettings(
                paddleConfig.getClientToken(),
                paddleConfig.isSandbox() ? "sandbox" : "production",
                plans);
    }
}
