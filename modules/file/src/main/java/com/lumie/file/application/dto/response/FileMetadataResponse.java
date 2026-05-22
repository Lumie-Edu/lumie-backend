package com.lumie.file.application.dto.response;

import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.vo.EntityType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record FileMetadataResponse(
        UUID id,
        EntityType entityType,
        @Schema(nullable = true) Long entityId,
        @Schema(nullable = true) UUID folderId,
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
                fileMetadata.getCreatedAt()
        );
    }
}
