package com.lumie.file.application.dto.response;

import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.vo.EntityType;

import java.time.Instant;
import com.lumie.common.util.Timestamps;
import java.util.UUID;

public record FileMetadataResponse(
        UUID id,
        EntityType entityType,
        Long entityId,
        UUID folderId,
        String originalFilename,
        String contentType,
        Long fileSize,
        boolean uploadCompleted,
        Instant createdAt
) {
    public static FileMetadataResponse from(FileMetadata fileMetadata) {
        return new FileMetadataResponse(
                fileMetadata.getId(),
                fileMetadata.getEntityType(),
                fileMetadata.getEntityId(),
                fileMetadata.getFolderId(),
                fileMetadata.getOriginalFilename(),
                fileMetadata.getContentType(),
                fileMetadata.getFileSize(),
                fileMetadata.isUploadCompleted(),
                Timestamps.utcInstant(fileMetadata.getCreatedAt())
        );
    }
}
