package com.lumie.file.adapter.in.scheduling;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.file.application.service.TemporaryUploadService;
import com.lumie.tenant.api.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Removes expired temporary-upload metadata and objects for every active tenant. */
@Slf4j
@Component
@RequiredArgsConstructor
public class TemporaryUploadCleanupScheduler {

    private final TenantService tenantService;
    private final TemporaryUploadService temporaryUploadService;

    @Scheduled(fixedDelayString = "PT10M")
    @SchedulerLock(name = "file-temporary-upload-cleanup", lockAtMostFor = "PT9M", lockAtLeastFor = "PT30S")
    public void cleanupExpiredUploads() {
        for (TenantService.TenantData tenant : tenantService.listActiveTenants()) {
            try {
                TenantContextHolder.withinContext(tenant.slug(), tenant.id(), temporaryUploadService::cleanupExpired);
            } catch (Exception exception) {
                log.error("Failed temporary-upload cleanup for tenant {}", tenant.slug(), exception);
            }
        }
    }
}
