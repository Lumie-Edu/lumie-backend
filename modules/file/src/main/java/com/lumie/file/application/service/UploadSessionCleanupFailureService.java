package com.lumie.file.application.service;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.file.application.event.UploadSessionCleanupRetryEvent;
import com.lumie.file.domain.entity.FileUploadSession;
import com.lumie.file.domain.repository.FileUploadSessionRepository;
import com.lumie.file.domain.vo.UploadSessionStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UploadSessionCleanupFailureService {
    private static final int MAX_ATTEMPTS = 5;
    private final FileUploadSessionRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public boolean recordAndScheduleRetry(UUID id, String error) {
        FileUploadSession session = repository.findByIdForUpdate(id).orElse(null);
        if (session == null || session.getStatus() == UploadSessionStatus.CLAIMED
                || session.getStatus() == UploadSessionStatus.CLEANED) return false;
        session.markCleanupFailed(error);
        if (session.getCleanupAttempts() >= MAX_ATTEMPTS) {
            eventPublisher.publishEvent(new UploadSessionCleanupRetryEvent(session.getId(), session.getTenantId(),
                    TenantContextHolder.getRequiredTenant(), Instant.now(), session.getCleanupAttempts()));
            return false;
        }
        eventPublisher.publishEvent(new UploadSessionCleanupRetryEvent(session.getId(), session.getTenantId(),
                TenantContextHolder.getRequiredTenant(), Instant.now().plusSeconds(30),
                session.getCleanupAttempts()));
        return true;
    }
}
