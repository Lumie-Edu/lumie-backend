package com.lumie.notification.adapter.in.scheduling;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.notification.application.service.SmsDispatchService;
import com.lumie.tenant.api.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsDispatchScheduler {

    private final SmsDispatchService dispatchService;
    private final TenantService tenantService;

    @Scheduled(fixedDelayString = "${lumie.sms.dispatch-fixed-delay-ms:60000}")
    @SchedulerLock(name = "sms-dispatch-due-messages", lockAtMostFor = "PT5M", lockAtLeastFor = "PT10S")
    public void dispatchDueMessages() {
        Instant now = Instant.now();
        for (TenantService.TenantData tenant : tenantService.listActiveTenants()) {
            try {
                TenantContextHolder.setTenant(tenant.slug());
                TenantContextHolder.setTenantId(tenant.id());
                MDC.put("tenantSlug", tenant.slug());
                MDC.put("tenantId", String.valueOf(tenant.id()));
                int dispatched = dispatchService.dispatchDueMessages(now);
                if (dispatched > 0) {
                    log.info("SMS dispatcher processed tenant={} count={}", tenant.slug(), dispatched);
                }
            } catch (Exception e) {
                log.error("SMS dispatcher failed for tenant {}: {}", tenant.slug(), e.getMessage(), e);
            } finally {
                TenantContextHolder.clear();
                MDC.remove("tenantSlug");
                MDC.remove("tenantId");
            }
        }
    }
}
