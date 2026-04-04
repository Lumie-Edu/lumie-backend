package com.lumie.tenant.adapter.out.internal;

import com.lumie.internal.billing.BillingInternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingInternalAdapter {

    private final BillingInternalService billingInternalService;

    public void createFreeSubscription(Long tenantId, String tenantSlug) {
        log.debug("Creating free subscription via internal API for tenant: {}", tenantSlug);
        billingInternalService.createFreeSubscription(tenantId, tenantSlug);
        log.info("Free subscription created for tenant: {}", tenantSlug);
    }
}
