package com.lumie.file.domain.entity;

import com.lumie.common.domain.BaseEntity;
import com.lumie.file.domain.vo.EntityType;
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
public class FileMetadata extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_slug", length = 50)
    private String tenantSlug;

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

    @Builder
    private FileMetadata(String tenantSlug, EntityType entityType, Long entityId,
                         String originalFilename, String storedFilename, String contentType,
                         Long fileSize, String objectKey, boolean uploadCompleted) {
        this.tenantSlug = tenantSlug;
        this.entityType = entityType;
        this.entityId = entityId;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.objectKey = objectKey;
        this.uploadCompleted = uploadCompleted;
    }

    public static FileMetadata create(String tenantSlug, EntityType entityType, Long entityId,
                                       String originalFilename, String contentType, Long fileSize,
                                       String objectKey) {
        String storedFilename = generateStoredFilename(originalFilename);
        return FileMetadata.builder()
                .tenantSlug(tenantSlug)
                .entityType(entityType)
                .entityId(entityId)
                .originalFilename(originalFilename)
                .storedFilename(storedFilename)
                .contentType(contentType)
                .fileSize(fileSize)
                .objectKey(objectKey)
                .uploadCompleted(false)
                .build();
    }

    public static FileMetadata createForPlatform(EntityType entityType, String originalFilename,
                                                  String contentType, Long fileSize, String objectKey) {
        return create(null, entityType, null, originalFilename, contentType, fileSize, objectKey);
    }

    public void markUploadCompleted() {
        this.uploadCompleted = true;
    }

    public void linkToEntity(Long entityId) {
        this.entityId = entityId;
    }

    public boolean isPlatformFile() {
        return tenantSlug == null;
    }

    private static String generateStoredFilename(String originalFilename) {
        int lastDotIndex = originalFilename.lastIndexOf('.');
        String extension = lastDotIndex > 0 ? originalFilename.substring(lastDotIndex) : "";
        return UUID.randomUUID() + extension;
    }
}
