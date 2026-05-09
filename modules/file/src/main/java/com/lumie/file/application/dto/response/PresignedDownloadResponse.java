package com.lumie.file.application.dto.response;

import java.util.UUID;

public record PresignedDownloadResponse(
        UUID fileId,
        String downloadUrl,
        String filename,
        String contentType,
        int expiresInSeconds
) {
    public static PresignedDownloadResponse of(UUID fileId, String downloadUrl, String filename,
                                                String contentType, int expiresInSeconds) {
        return new PresignedDownloadResponse(fileId, downloadUrl, filename, contentType, expiresInSeconds);
    }
}
