package com.lumie.tuition.adapter.in.scheduling;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.tuition.application.service.TuitionInvoiceCommandService;
import com.lumie.tenant.api.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Daily scheduler that detects overdue tuition invoices and notifies academy owners.
 *
 * <p>Strategy:
 * <ol>
 *   <li>Iterate all active tenants (via TenantService from libs/internal-api).</li>
 *   <li>Within each tenant context (RLS-safe), delegate to TuitionInvoiceCommandService
 *       which queries ISSUED invoices past dueDate and marks them OVERDUE.</li>
 *   <li>Dispatch owner notification (via NotificationService from libs/internal-api — Task 5).</li>
 * </ol>
 *
 * <p>Guardian/student push notifications for payment reminders are NOT sent automatically —
 * academy owners are notified only. Direct guardian nudges require explicit owner action.
 *
 * <p>TODO (Task 5): inject {@code NotificationService} from {@code libs/internal-api}
 * and replace the log stub with an actual notification dispatch.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueNotificationScheduler {

    private final TuitionInvoiceCommandService invoiceCommandService;
    private final TenantService tenantService;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    @SchedulerLock(name = "tuition-overdue-notify", lockAtMostFor = "PT20M", lockAtLeastFor = "PT1M")
    public void notifyOverdueInvoices() {
        log.info("OverdueNotificationScheduler: starting daily overdue check");

        List<TenantService.TenantData> tenants = tenantService.listActiveTenants();
        int totalMarked = 0;

        for (TenantService.TenantData tenant : tenants) {
            try {
                TenantContextHolder.setTenant(tenant.slug());
                TenantContextHolder.setTenantId(tenant.id());
                MDC.put("tenantSlug", tenant.slug());
                MDC.put("tenantId", String.valueOf(tenant.id()));

                // Delegate to service bean — @Transactional applied via Spring proxy correctly
                int marked = invoiceCommandService.markOverdueInvoices();
                totalMarked += marked;
                log.info("OverdueNotificationScheduler: tenant={}, invoicesMarked={}", tenant.slug(), marked);
                // TODO (Task 5): dispatch notification to academy owner for each marked invoice
            } catch (Exception e) {
                log.error("OverdueNotificationScheduler: error for tenant {}: {}",
                        tenant.slug(), e.getMessage(), e);
            } finally {
                TenantContextHolder.clear();
                MDC.remove("tenantSlug");
                MDC.remove("tenantId");
            }
        }

        log.info("OverdueNotificationScheduler: completed. total overdue marked={}", totalMarked);
    }
}
