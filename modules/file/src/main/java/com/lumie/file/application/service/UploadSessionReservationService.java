package com.lumie.file.application.service;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.file.application.dto.request.CreateUploadSessionRequest;
import com.lumie.file.application.event.UploadSessionCleanupDueEvent;
import com.lumie.file.domain.entity.FileUploadSession;
import com.lumie.file.domain.repository.FileUploadSessionRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UploadSessionReservationService {
    private final FileUploadSessionRepository sessionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public FileUploadSession reserve(UUID id, String objectKey, CreateUploadSessionRequest request,
                                     Instant uploadUrlExpiresAt, Instant sessionExpiresAt) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        FileUploadSession session = sessionRepository.save(FileUploadSession.issue(id,
                UserContextHolder.getRequiredUserId(), request.entityType(), objectKey, request.filename(),
                request.contentType(), request.fileSize(), uploadUrlExpiresAt, sessionExpiresAt));
        eventPublisher.publishEvent(new UploadSessionCleanupDueEvent(id, tenantId, tenantSlug,
                sessionExpiresAt.plusSeconds(15 * 60)));
        return session;
    }
}
