package com.lumie.file.application.service;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.file.application.dto.message.UploadSessionCleanupMessage;
import com.lumie.file.application.port.out.StoragePort;
import com.lumie.file.domain.entity.FileUploadSession;
import com.lumie.file.domain.vo.UploadSessionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadSessionCleanupService {
    private final UploadSessionStateService stateService;
    private final UploadSessionCleanupFailureService failureService;
    private final StoragePort storagePort;

    public void clean(UploadSessionCleanupMessage message) {
        TenantContextHolder.withinContext(message.tenantSlug(), message.tenantId(), () -> cleanForCurrentTenant(message));
    }

    private void cleanForCurrentTenant(UploadSessionCleanupMessage message) {
        if (message.cleanupNotBefore() != null && java.time.Instant.now().isBefore(message.cleanupNotBefore())) {
            throw new IllegalStateException("Upload cleanup message was delivered before its due time");
        }
        FileUploadSession session = stateService.beginCleanup(message.sessionId());
        if (session.getStatus() == UploadSessionStatus.CLAIMED || session.getStatus() == UploadSessionStatus.CLEANED) return;
        try {
            storagePort.deleteObject(session.getObjectKey());
            stateService.finishCleanup(message.sessionId(), true, null);
            log.info("Unclaimed upload object removed sessionId={} tenantId={}",
                    message.sessionId(), message.tenantId());
        } catch (RuntimeException exception) {
            boolean retryScheduled = failureService.recordAndScheduleRetry(message.sessionId(), exception.getMessage());
            if (!retryScheduled) {
                log.error("Upload session cleanup reached terminal failure sessionId={} tenantId={}",
                        message.sessionId(), message.tenantId(), exception);
            } else {
                log.warn("Upload session cleanup failed; durable retry scheduled sessionId={} attempt={}",
                        message.sessionId(), message.attempt() + 1, exception);
            }
        }
    }
}
