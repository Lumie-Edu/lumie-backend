package com.lumie.file.application.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PresignedDownloadRequest(
        @NotNull(message = "File ID is required")
        UUID fileId
) {
}
