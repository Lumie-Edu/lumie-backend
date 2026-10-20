package com.lumie.file.adapter.in.scheduling;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.file.application.service.StorageOperationProcessor;
import com.lumie.file.application.service.StorageOperationMetrics;
import com.lumie.tenant.api.TenantService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Component
@RequiredArgsConstructor
public class StorageOperationScheduler {

    private final StorageOperationProcessor processor;
    private final StorageOperationMetrics metrics;
    private final TenantService tenantService;
    @Value("${lumie.file.storage-operation.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${lumie.file.storage-operation.poll-interval-ms:30000}")
    @SchedulerLock(name = "file-storage-operation-processor", lockAtMostFor = "PT5M", lockAtLeastFor = "PT10S")
    public void processDueOperations() {
        String workerId = UUID.randomUUID().toString();
        metrics.beginSnapshotRefresh();
        for (TenantService.TenantData tenant : tenantService.listActiveTenants()) {
            try {
                TenantContextHolder.setTenant(tenant.slug());
                TenantContextHolder.setTenantId(tenant.id());
                MDC.put("tenantSlug", tenant.slug());
                MDC.put("tenantId", String.valueOf(tenant.id()));
                int processed = processor.processDueOperations(batchSize, workerId);
                metrics.refreshCurrentTenantSnapshot();
                if (processed > 0) {
                    log.info("File storage operation processor handled tenant={} count={} worker={}",
                            tenant.slug(), processed, workerId);
                }
            } catch (Exception exception) {
                log.error("File storage operation processor failed for tenant {}", tenant.slug(), exception);
            } finally {
                TenantContextHolder.clear();
                MDC.remove("tenantSlug");
                MDC.remove("tenantId");
            }
        }
    }
}
