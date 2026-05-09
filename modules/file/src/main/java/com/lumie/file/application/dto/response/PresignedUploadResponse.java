package com.lumie.file.application.dto.response;

import java.util.UUID;

public record PresignedUploadResponse(
        UUID fileId,
        String uploadUrl,
        int expiresInSeconds
) {
    public static PresignedUploadResponse of(UUID fileId, String uploadUrl, int expiresInSeconds) {
        return new PresignedUploadResponse(fileId, uploadUrl, expiresInSeconds);
    }
}
