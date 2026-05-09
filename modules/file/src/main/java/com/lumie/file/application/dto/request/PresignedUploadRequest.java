package com.lumie.file.application.dto.request;

import com.lumie.file.domain.vo.EntityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PresignedUploadRequest(
        @NotNull(message = "Entity type is required")
        EntityType entityType,

        Long entityId,

        @NotBlank(message = "Filename is required")
        String filename,

        @NotBlank(message = "Content type is required")
        String contentType,

        @NotNull(message = "File size is required")
        @Positive(message = "File size must be positive")
        Long fileSize
) {
}
