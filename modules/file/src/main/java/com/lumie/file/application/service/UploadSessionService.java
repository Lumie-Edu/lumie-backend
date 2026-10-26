package com.lumie.file.application.service;

import com.lumie.common.auth.Role;
import com.lumie.common.auth.AuthorizationGuard;
import com.lumie.common.exception.BusinessException;
import com.lumie.file.application.dto.request.CreateUploadSessionRequest;
import com.lumie.file.application.dto.response.UploadSessionResponse;
import com.lumie.file.application.dto.response.UploadSessionStatusResponse;
import com.lumie.file.application.port.out.StoragePort;
import com.lumie.file.domain.entity.FileUploadSession;
import com.lumie.file.domain.exception.FileErrorCode;
import com.lumie.file.domain.vo.EntityType;
import com.lumie.file.domain.vo.FilePath;
import com.lumie.file.domain.vo.UploadSessionStatus;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UploadSessionService {
    private static final long SESSION_SECONDS = 24 * 60 * 60;
    private static final int URL_SECONDS = 15 * 60;
    private static final Set<EntityType> SUPPORTED_TYPES = Set.of(EntityType.ANNOUNCEMENT, EntityType.QNA, EntityType.TEXTBOOK);

    private final StoragePort storage;
    private final UploadSessionReservationService reservationService;
    private final UploadSessionStateService stateService;

    public UploadSessionResponse create(CreateUploadSessionRequest request) {
        validate(request);
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        Instant urlExpiry = now.plusSeconds(URL_SECONDS);
        Instant sessionExpiry = now.plusSeconds(SESSION_SECONDS);
        String objectKey = FilePath.of(request.entityType(), id, request.filename()).toObjectKey();

        // Reservation and cleanup event commit before the browser receives this URL.
        reservationService.reserve(id, objectKey, request, urlExpiry, sessionExpiry);
        String url = storage.generatePresignedUploadUrl(objectKey, request.contentType(), URL_SECONDS);
        return new UploadSessionResponse(id, objectKey, url, urlExpiry, sessionExpiry, "ISSUED");
    }

    public UploadSessionStatusResponse complete(UUID id) {
        FileUploadSession session = stateService.findAuthorized(id);
        if (session.getStatus() == UploadSessionStatus.CLAIMED) return toResponse(session);
        if (session.getStatus() != UploadSessionStatus.ISSUED && session.getStatus() != UploadSessionStatus.UPLOADED) {
            throw invalidSession();
        }
        if (!storage.objectExists(session.getObjectKey())
                || storage.objectSize(session.getObjectKey()) != session.getFileSize()) throw invalidSession();
        String actualType = storage.objectContentType(session.getObjectKey());
        String etag = storage.objectEtag(session.getObjectKey());
        if (actualType == null || !actualType.equalsIgnoreCase(session.getContentType()) || etag == null || etag.isBlank()) {
            throw new BusinessException(FileErrorCode.INVALID_FILE_DATA);
        }
        if (session.getStatus() == UploadSessionStatus.UPLOADED
                && session.getUploadedEtag() != null && !session.getUploadedEtag().equals(etag)) {
            throw invalidSession();
        }
        return stateService.markUploaded(id, etag);
    }

    public UploadSessionStatusResponse status(UUID id) {
        FileUploadSession session = stateService.findAuthorized(id);
        if (session.getStatus() != UploadSessionStatus.CLAIMED
                && !Instant.now().isBefore(session.getSessionExpiresAt())) return stateService.expire(id);
        if (session.getStatus() == UploadSessionStatus.ISSUED && storage.objectExists(session.getObjectKey())) {
            return complete(id);
        }
        return toResponse(session);
    }

    public UploadSessionResponse renew(UUID id) {
        FileUploadSession session = stateService.findAuthorized(id);
        if (session.getStatus() != UploadSessionStatus.ISSUED || !Instant.now().isBefore(session.getSessionExpiresAt())) {
            throw invalidSession();
        }
        if (storage.objectExists(session.getObjectKey())) {
            complete(id);
            throw invalidSession();
        }
        Instant expiry = Instant.now().plusSeconds(URL_SECONDS);
        if (expiry.isAfter(session.getSessionExpiresAt())) throw invalidSession();
        stateService.renew(id, expiry);
        String url = storage.generatePresignedUploadUrl(session.getObjectKey(), session.getContentType(), URL_SECONDS);
        return new UploadSessionResponse(id, session.getObjectKey(), url, expiry,
                session.getSessionExpiresAt(), "ISSUED");
    }

    public void cancel(UUID id) {
        stateService.cancel(id);
    }

    private void validate(CreateUploadSessionRequest request) {
        if (request == null || request.entityType() == null || !SUPPORTED_TYPES.contains(request.entityType())
                || request.fileSize() == null || request.fileSize() <= 0
                || request.fileSize() > TemporaryObjectUploadService.MAX_BYTES
                || !TemporaryObjectUploadService.isAllowedFilename(request.filename())
                || request.contentType() == null || request.contentType().isBlank()) {
            throw new BusinessException(FileErrorCode.INVALID_FILE_DATA);
        }
        Role role = com.lumie.common.tenant.UserContextHolder.getUserRole();
        if (request.entityType() == EntityType.QNA && role == Role.STUDENT) return;
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
    }

    private UploadSessionStatusResponse toResponse(FileUploadSession session) {
        return new UploadSessionStatusResponse(session.getId(), session.getStatus().name(), session.getFilename(),
                session.getContentType(), session.getFileSize(), session.getUploadUrlExpiresAt(), session.getSessionExpiresAt());
    }

    private BusinessException invalidSession() {
        return new BusinessException(FileErrorCode.UPLOAD_SESSION_INVALID);
    }
}
