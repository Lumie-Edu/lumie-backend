package com.lumie.file.application.dto.request;

import com.lumie.file.domain.vo.EntityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateTemporaryUploadRequest(
        @NotNull EntityType entityType,
        @NotBlank String filename,
        @NotBlank String contentType,
        @NotNull @Positive Long fileSize
) {}
