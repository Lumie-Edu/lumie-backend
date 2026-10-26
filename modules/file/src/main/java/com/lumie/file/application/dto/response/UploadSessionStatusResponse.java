package com.lumie.file.application.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UploadSessionStatusResponse(UUID sessionId, String status, String filename,
        String contentType, long fileSize, Instant uploadUrlExpiresAt, Instant sessionExpiresAt) {}
