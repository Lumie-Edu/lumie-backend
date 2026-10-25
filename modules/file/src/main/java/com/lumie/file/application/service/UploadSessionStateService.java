package com.lumie.file.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.file.application.dto.response.UploadSessionStatusResponse;
import com.lumie.file.domain.entity.FileUploadSession;
import com.lumie.file.domain.exception.FileErrorCode;
import com.lumie.file.domain.repository.FileUploadSessionRepository;
import com.lumie.file.domain.vo.UploadSessionStatus;
import com.lumie.file.domain.vo.EntityType;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UploadSessionStateService {
    private final FileUploadSessionRepository repository;

    @Transactional(readOnly = true)
    public FileUploadSession findAuthorized(UUID id) {
        FileUploadSession session = repository.findById(id).orElseThrow(this::invalidSession);
        authorize(session);
        return session;
    }

    @Transactional
    public UploadSessionStatusResponse markUploaded(UUID id, String etag) {
        FileUploadSession session = repository.findByIdForUpdate(id).orElseThrow(this::invalidSession);
        authorize(session);
        if (session.getStatus() == UploadSessionStatus.CLAIMED) return response(session);
        if ((session.getStatus() != UploadSessionStatus.ISSUED && session.getStatus() != UploadSessionStatus.UPLOADED)
                || !Instant.now().isBefore(session.getSessionExpiresAt())) throw invalidSession();
        session.markUploaded(etag);
        return response(session);
    }

    @Transactional
    public void renew(UUID id, Instant expiresAt) {
        FileUploadSession session = repository.findByIdForUpdate(id).orElseThrow(this::invalidSession);
        authorize(session);
        if (session.getStatus() != UploadSessionStatus.ISSUED
                || !Instant.now().isBefore(session.getSessionExpiresAt())
                || expiresAt.isAfter(session.getSessionExpiresAt())) throw invalidSession();
        session.renewUploadUrlUntil(expiresAt);
    }

    @Transactional
    public void cancel(UUID id) {
        FileUploadSession session = repository.findByIdForUpdate(id).orElseThrow(this::invalidSession);
        authorize(session);
        session.requestCancellation();
    }

    @Transactional
    public UploadSessionStatusResponse expire(UUID id) {
        FileUploadSession session = repository.findByIdForUpdate(id).orElseThrow(this::invalidSession);
        authorize(session);
        session.markExpired(Instant.now());
        return response(session);
    }

    @Transactional
    public FileUploadSession beginCleanup(UUID id) {
        FileUploadSession session = repository.findByIdForUpdate(id).orElseThrow(this::invalidSession);
        if (session.getStatus() == UploadSessionStatus.CLAIMED || session.getStatus() == UploadSessionStatus.CLEANED) {
            return session;
        }
        session.beginCleanup(Instant.now());
        return session;
    }

    @Transactional
    public void finishCleanup(UUID id, boolean success, String error) {
        FileUploadSession session = repository.findByIdForUpdate(id).orElseThrow(this::invalidSession);
        if (session.getStatus() == UploadSessionStatus.CLAIMED || session.getStatus() == UploadSessionStatus.CLEANED) return;
        if (success) session.markCleaned(); else session.markCleanupFailed(error);
    }

    @Transactional
    public void claim(UUID id, UUID fileId, EntityType entityType) {
        FileUploadSession session = repository.findByIdForUpdate(id).orElseThrow(this::invalidSession);
        authorize(session);
        if (!Instant.now().isBefore(session.getSessionExpiresAt()) || session.getEntityType() != entityType) {
            throw invalidSession();
        }
        session.claim(fileId);
    }

    private void authorize(FileUploadSession session) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null || !session.getInitiatingUserId().equals(userId)
                || !session.getTenantId().equals(TenantContextHolder.getRequiredTenantId())) throw invalidSession();
    }

    private UploadSessionStatusResponse response(FileUploadSession session) {
        return new UploadSessionStatusResponse(session.getId(), session.getStatus().name(), session.getFilename(),
                session.getContentType(), session.getFileSize(), session.getUploadUrlExpiresAt(),
                session.getSessionExpiresAt());
    }

    private BusinessException invalidSession() {
        return new BusinessException(FileErrorCode.UPLOAD_SESSION_INVALID);
    }
}
