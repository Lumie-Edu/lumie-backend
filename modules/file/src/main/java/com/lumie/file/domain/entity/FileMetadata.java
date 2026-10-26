package com.lumie.file.domain.entity;

import com.lumie.common.domain.TenantScopedEntity;
import com.lumie.file.domain.vo.EntityType;
import com.lumie.file.domain.vo.FileUploadStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "file_metadata")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileMetadata extends TenantScopedEntity {

    @Id
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

    @Column(name = "upload_completed", nullable = false)
    private boolean uploadCompleted;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_status", nullable = false, length = 16)
    private FileUploadStatus uploadStatus;

    @Column(name = "folder_id")
    private UUID folderId;


    @Builder
    private FileMetadata(EntityType entityType, Long entityId,
                         String originalFilename, String storedFilename, String contentType,
                         Long fileSize, String objectKey, UUID folderId) {
        this.id = UUID.randomUUID();
        this.entityType = entityType;
        this.entityId = entityId;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.objectKey = objectKey;
        this.uploadCompleted = false;
        this.uploadStatus = FileUploadStatus.PENDING;
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
                .folderId(folderId)
                .build();
    }

    public static FileMetadata createWithId(UUID id, EntityType entityType, Long entityId,
                                             String originalFilename, String contentType, Long fileSize,
                                             String objectKey, UUID folderId) {
        FileMetadata metadata = create(entityType, entityId, originalFilename, contentType, fileSize, objectKey, folderId);
        metadata.id = id;
        return metadata;
    }

    public void markUploadCompleted() {
        this.uploadStatus = FileUploadStatus.READY;
        this.uploadCompleted = true;
    }

    public void markReady() { markUploadCompleted(); }

    public void markFailed() {
        this.uploadStatus = FileUploadStatus.FAILED;
        this.uploadCompleted = false;
    }

    public void markDeleting() {
        this.uploadStatus = FileUploadStatus.DELETING;
        this.uploadCompleted = false;
    }

    public void clearFolder() {
        this.folderId = null;
    }

    public boolean isDeleting() {
        return uploadStatus == FileUploadStatus.DELETING;
    }

    public boolean isReady() {
        return uploadStatus == FileUploadStatus.READY && uploadCompleted;
    }


    private static String generateStoredFilename(String originalFilename) {
        int lastDotIndex = originalFilename.lastIndexOf('.');
        String extension = lastDotIndex > 0 ? originalFilename.substring(lastDotIndex) : "";
        return UUID.randomUUID() + extension;
    }
}
