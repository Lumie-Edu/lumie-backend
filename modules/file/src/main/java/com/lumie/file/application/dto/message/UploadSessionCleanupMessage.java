package com.lumie.file.application.dto.message;

import java.time.Instant;
import java.util.UUID;

public record UploadSessionCleanupMessage(UUID sessionId, Long tenantId, String tenantSlug,
                                          Instant cleanupNotBefore, int attempt) {}
