package com.lumie.file.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateUploadSessionRequest(
        @NotNull UploadableEntityType entityType,
        @NotBlank String filename,
        @NotBlank String contentType,
        @NotNull @Positive Long fileSize
) {}
