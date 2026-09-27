package com.lumie.file.application.dto.response;

import java.time.Instant;
import java.util.UUID;

public record TemporaryUploadResponse(UUID fileId, String uploadUrl, Instant expiresAt) {}
