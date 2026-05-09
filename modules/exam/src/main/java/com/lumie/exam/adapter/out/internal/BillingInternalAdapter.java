package com.lumie.exam.adapter.out.internal;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.application.port.out.BillingServicePort;
import com.lumie.internal.billing.BillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("examBillingInternalAdapter")
@RequiredArgsConstructor
public class BillingInternalAdapter implements BillingServicePort {

    private static final String OMR_METRIC_TYPE = "OMR_GRADING";

    private final BillingService billingService;

    @Override
    public boolean hasOmrQuota() {
        String slug = TenantContextHolder.getRequiredTenant();
        log.debug("Checking OMR quota via internal API for tenant: {}", slug);
        return billingService.checkQuota(slug, OMR_METRIC_TYPE).allowed();
    }

    @Override
    public void consumeOmrQuota() {
        // Quota consumption is handled by billing-svc when recording usage
        log.debug("OMR quota consumption recorded");
    }

    @Override
    public OmrQuotaInfo getOmrQuotaInfo() {
        String slug = TenantContextHolder.getRequiredTenant();
        log.debug("Getting OMR quota info via internal API for tenant: {}", slug);
        var result = billingService.checkQuota(slug, OMR_METRIC_TYPE);
        int limit = (int) result.limit();
        int used = (int) result.currentUsage();
        return new OmrQuotaInfo(used, limit, limit - used);
    }
}
