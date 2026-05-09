package com.lumie.tenant.adapter.out.internal;

import com.lumie.internal.billing.BillingService;
import com.lumie.tenant.application.port.out.BillingServicePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("tenantBillingInternalAdapter")
@RequiredArgsConstructor
public class BillingInternalAdapter implements BillingServicePort {

    private final BillingService billingService;

    @Override
    public void createFreeSubscription(Long tenantId, String tenantSlug) {
        log.debug("Creating free subscription via internal API for tenant: {}", tenantSlug);
        billingService.createFreeSubscription(tenantId, tenantSlug);
        log.info("Free subscription created for tenant: {}", tenantSlug);
    }
}
