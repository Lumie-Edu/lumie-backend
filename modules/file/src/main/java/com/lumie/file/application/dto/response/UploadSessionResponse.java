package com.lumie.file.application.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UploadSessionResponse(UUID sessionId, String objectKey, String uploadUrl,
                                   Instant uploadUrlExpiresAt, Instant sessionExpiresAt, String status) {}
