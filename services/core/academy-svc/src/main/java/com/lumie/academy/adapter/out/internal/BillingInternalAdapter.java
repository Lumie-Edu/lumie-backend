package com.lumie.academy.adapter.out.internal;

import com.lumie.academy.application.port.out.BillingServicePort;
import com.lumie.internal.billing.BillingInternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("academyBillingInternalAdapter")
@RequiredArgsConstructor
public class BillingInternalAdapter implements BillingServicePort {

    private final BillingInternalService billingInternalService;

    @Override
    public QuotaCheckResult checkQuota(String tenantSlug, String metricType) {
        log.debug("Checking quota via internal API for tenant: {}, metric: {}", tenantSlug, metricType);
        var result = billingInternalService.checkQuota(tenantSlug, metricType);
        return new QuotaCheckResult(result.allowed(), result.currentUsage(), result.limit(), result.message());
    }
}
