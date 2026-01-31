package com.lumie.file.application.dto.response;

import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.vo.EntityType;

import java.time.LocalDateTime;
import java.util.UUID;

public record FileMetadataResponse(
        UUID id,
        EntityType entityType,
        Long entityId,
        String originalFilename,
        String contentType,
        Long fileSize,
        boolean uploadCompleted,
        LocalDateTime createdAt
) {
    public static FileMetadataResponse from(FileMetadata fileMetadata) {
        return new FileMetadataResponse(
                fileMetadata.getId(),
                fileMetadata.getEntityType(),
                fileMetadata.getEntityId(),
                fileMetadata.getOriginalFilename(),
                fileMetadata.getContentType(),
                fileMetadata.getFileSize(),
                fileMetadata.isUploadCompleted(),
                fileMetadata.getCreatedAt()
        );
    }
}
