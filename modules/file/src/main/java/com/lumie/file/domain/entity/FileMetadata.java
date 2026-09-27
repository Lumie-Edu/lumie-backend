package com.lumie.file.domain.entity;

import com.lumie.common.domain.TenantScopedEntity;
import com.lumie.file.domain.vo.EntityType;
import com.lumie.file.domain.vo.FileLifecycleStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.time.Instant;

@Entity
@Table(name = "file_metadata")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileMetadata extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 20)
    private EntityType entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, length = 255)
    private String storedFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "object_key", nullable = false, length = 500)
    private String objectKey;

    /** Original tmp object kept until the successful attach is committed. */
    @Column(name = "temporary_object_key", length = 500)
    private String temporaryObjectKey;

    @Column(name = "upload_completed", nullable = false)
    private boolean uploadCompleted;

    @Column(name = "folder_id")
    private UUID folderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status", nullable = false, length = 20)
    private FileLifecycleStatus lifecycleStatus = FileLifecycleStatus.ACTIVE;

    @Column(name = "uploaded_by_user_id")
    private Long uploadedByUserId;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "completed_etag", length = 128)
    private String completedEtag;

    @Builder
    private FileMetadata(EntityType entityType, Long entityId,
                         String originalFilename, String storedFilename, String contentType,
                         Long fileSize, String objectKey, boolean uploadCompleted, UUID folderId) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.objectKey = objectKey;
        this.uploadCompleted = uploadCompleted;
        this.folderId = folderId;
    }

    public static FileMetadata create(EntityType entityType, Long entityId,
                                       String originalFilename, String contentType, Long fileSize,
                                       String objectKey, UUID folderId) {
        String storedFilename = generateStoredFilename(originalFilename);
        return FileMetadata.builder()
                .entityType(entityType)
                .entityId(entityId)
                .originalFilename(originalFilename)
                .storedFilename(storedFilename)
                .contentType(contentType)
                .fileSize(fileSize)
                .objectKey(objectKey)
                .uploadCompleted(false)
                .folderId(folderId)
                .build();
    }

    public void markUploadCompleted() {
        this.uploadCompleted = true;
    }

    public void markUploadCompleted(String etag) {
        this.uploadCompleted = true;
        this.completedEtag = etag;
    }

    public void linkToEntity(Long entityId) {
        if (lifecycleStatus != FileLifecycleStatus.READY_TO_ATTACH) {
            throw new IllegalStateException("Only prepared temporary files can be attached");
        }
        this.entityId = entityId;
        this.lifecycleStatus = FileLifecycleStatus.ACTIVE;
        this.expiresAt = null;
        this.temporaryObjectKey = null;
    }

    public static FileMetadata createTemporary(EntityType entityType, String filename, String contentType,
                                               Long fileSize, String objectKey, Long uploadedByUserId, Instant expiresAt) {
        FileMetadata file = create(entityType, null, filename, contentType, fileSize, objectKey, null);
        file.lifecycleStatus = FileLifecycleStatus.TEMPORARY;
        file.uploadedByUserId = uploadedByUserId;
        file.expiresAt = expiresAt;
        return file;
    }

    public boolean isTemporaryOwnedBy(Long userId) {
        return (lifecycleStatus == FileLifecycleStatus.TEMPORARY || lifecycleStatus == FileLifecycleStatus.READY_TO_ATTACH)
                && uploadedByUserId != null
                && uploadedByUserId.equals(userId) && expiresAt != null && expiresAt.isAfter(Instant.now());
    }

    public void markDeleting() { this.lifecycleStatus = FileLifecycleStatus.DELETING; }

    public boolean isTemporary() {
        return lifecycleStatus == FileLifecycleStatus.TEMPORARY || lifecycleStatus == FileLifecycleStatus.READY_TO_ATTACH;
    }

    public void markPrepared(String permanentObjectKey) {
        if (lifecycleStatus != FileLifecycleStatus.TEMPORARY || !uploadCompleted) {
            throw new IllegalStateException("Only completed temporary files can be prepared");
        }
        this.temporaryObjectKey = this.objectKey;
        this.objectKey = permanentObjectKey;
        this.lifecycleStatus = FileLifecycleStatus.READY_TO_ATTACH;
    }

    private static String generateStoredFilename(String originalFilename) {
        int lastDotIndex = originalFilename.lastIndexOf('.');
        String extension = lastDotIndex > 0 ? originalFilename.substring(lastDotIndex) : "";
        return UUID.randomUUID() + extension;
    }
}
