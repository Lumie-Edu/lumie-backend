package com.lumie.assignment.adapter.in.scheduling;

import com.lumie.assignment.application.service.AssignmentLifecycleService;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.tenant.api.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssignmentDeadlineScheduler {

    private final AssignmentLifecycleService lifecycleService;
    private final TenantService tenantService;

    @Scheduled(fixedDelayString = "${lumie.assignment.close-overdue-fixed-delay-ms:60000}")
    @SchedulerLock(name = "assignment-close-overdue", lockAtMostFor = "PT5M", lockAtLeastFor = "PT10S")
    public void closeOverdueAssignments() {
        for (TenantService.TenantData tenant : tenantService.listActiveTenants()) {
            try {
                TenantContextHolder.setTenant(tenant.slug());
                TenantContextHolder.setTenantId(tenant.id());
                MDC.put("tenantSlug", tenant.slug());
                MDC.put("tenantId", String.valueOf(tenant.id()));
                int closed = lifecycleService.closeOverdueAssignments();
                if (closed > 0) {
                    log.info("Assignment deadline scheduler closed tenant={} count={}", tenant.slug(), closed);
                }
            } catch (Exception e) {
                log.error("Assignment deadline scheduler failed for tenant {}: {}", tenant.slug(), e.getMessage(), e);
            } finally {
                TenantContextHolder.clear();
                MDC.remove("tenantSlug");
                MDC.remove("tenantId");
            }
        }
    }
}
