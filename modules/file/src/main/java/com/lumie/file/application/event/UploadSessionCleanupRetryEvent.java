package com.lumie.file.application.event;

import java.time.Instant;
import java.util.UUID;

public record UploadSessionCleanupRetryEvent(UUID sessionId, Long tenantId, String tenantSlug,
                                              Instant cleanupNotBefore, int attempt) {}
