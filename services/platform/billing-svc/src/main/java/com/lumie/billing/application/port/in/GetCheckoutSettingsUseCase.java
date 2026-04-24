package com.lumie.billing.application.port.in;

import java.util.Map;

public interface GetCheckoutSettingsUseCase {

    record CheckoutSettings(String clientToken, String environment, Map<String, PlanPriceIds> plans) {}

    record PlanPriceIds(String monthlyPriceId, String yearlyPriceId) {}

    CheckoutSettings getCheckoutSettings();
}
