package com.lumie.file.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import com.lumie.file.domain.vo.EntityType;
import io.swagger.v3.oas.annotations.media.Schema;

public record CreateUploadSessionRequest(
        @NotNull @Schema(allowableValues = {"ANNOUNCEMENT", "QNA"}) EntityType entityType,
        @NotBlank String filename,
        @NotBlank String contentType,
        @NotNull @Positive Long fileSize
) {}
