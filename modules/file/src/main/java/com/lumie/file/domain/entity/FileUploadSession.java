package com.lumie.file.domain.entity;

import com.lumie.common.domain.TenantScopedEntity;
import com.lumie.common.exception.BusinessException;
import com.lumie.file.domain.exception.FileErrorCode;
import com.lumie.file.domain.vo.EntityType;
import com.lumie.file.domain.vo.UploadSessionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "file_upload_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileUploadSession extends TenantScopedEntity {

    @Id
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "initiating_user_id", nullable = false)
    private Long initiatingUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 20)
    private EntityType entityType;

    @Column(name = "object_key", nullable = false, unique = true, length = 500)
    private String objectKey;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String filename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "upload_url_expires_at", nullable = false)
    private Instant uploadUrlExpiresAt;

    @Column(name = "session_expires_at", nullable = false)
    private Instant sessionExpiresAt;

    @Column(name = "uploaded_etag", length = 255)
    private String uploadedEtag;

    @Column(name = "claimed_file_id")
    private UUID claimedFileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private UploadSessionStatus status;

    @Column(name = "last_cleanup_error", length = 1000)
    private String lastCleanupError;

    @Column(name = "cleanup_attempts", nullable = false)
    private int cleanupAttempts;

    private FileUploadSession(UUID id, Long initiatingUserId, EntityType entityType, String objectKey,
                              String filename, String contentType, Long fileSize,
                              Instant uploadUrlExpiresAt, Instant sessionExpiresAt) {
        this.id = id;
        this.initiatingUserId = initiatingUserId;
        this.entityType = entityType;
        this.objectKey = objectKey;
        this.filename = filename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.uploadUrlExpiresAt = uploadUrlExpiresAt;
        this.sessionExpiresAt = sessionExpiresAt;
        this.status = UploadSessionStatus.ISSUED;
    }

    public static FileUploadSession issue(UUID id, Long initiatingUserId, EntityType entityType,
                                           String objectKey, String filename, String contentType,
                                           Long fileSize, Instant uploadUrlExpiresAt, Instant sessionExpiresAt) {
        return new FileUploadSession(id, initiatingUserId, entityType, objectKey, filename, contentType,
                fileSize, uploadUrlExpiresAt, sessionExpiresAt);
    }

    public void markUploaded(String etag) {
        if (status == UploadSessionStatus.CLAIMED || status == UploadSessionStatus.CLEANED) return;
        status = UploadSessionStatus.UPLOADED;
        uploadedEtag = etag;
    }

    public void renewUploadUrlUntil(Instant expiresAt) {
        if (status != UploadSessionStatus.ISSUED || Instant.now().isAfter(sessionExpiresAt)) {
            throw new BusinessException(FileErrorCode.UPLOAD_SESSION_INVALID);
        }
        uploadUrlExpiresAt = expiresAt;
    }

    public void claim(UUID fileId) {
        if (status != UploadSessionStatus.UPLOADED || claimedFileId != null) {
            throw new BusinessException(FileErrorCode.UPLOAD_SESSION_INVALID);
        }
        claimedFileId = fileId;
        status = UploadSessionStatus.CLAIMED;
    }

    public void requestCancellation() {
        if (status == UploadSessionStatus.CLAIMED || status == UploadSessionStatus.CLEANED) return;
        if (status != UploadSessionStatus.CLEANUP_PENDING) status = UploadSessionStatus.CANCEL_REQUESTED;
    }

    public void markExpired(Instant now) {
        if (status == UploadSessionStatus.CLAIMED || status == UploadSessionStatus.CLEANED) return;
        if (!now.isBefore(sessionExpiresAt)) status = UploadSessionStatus.EXPIRED;
    }

    public void beginCleanup(Instant now) {
        if (status == UploadSessionStatus.CLAIMED || status == UploadSessionStatus.CLEANED) return;
        if (now.isBefore(sessionExpiresAt.plusSeconds(15 * 60))) {
            throw new BusinessException(FileErrorCode.UPLOAD_SESSION_INVALID);
        }
        status = UploadSessionStatus.CLEANUP_PENDING;
    }

    public void markCleaned() {
        if (status == UploadSessionStatus.CLAIMED) return;
        status = UploadSessionStatus.CLEANED;
        lastCleanupError = null;
    }

    public void markCleanupFailed(String error) {
        if (status == UploadSessionStatus.CLAIMED || status == UploadSessionStatus.CLEANED) return;
        status = UploadSessionStatus.CLEANUP_FAILED;
        cleanupAttempts++;
        lastCleanupError = error == null ? "Storage cleanup failed" : error.substring(0, Math.min(error.length(), 1000));
    }
}
