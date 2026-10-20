package com.lumie.file.api;

import java.time.Instant;
import java.util.UUID;

/** Validated upload metadata prepared for a later database attachment transaction. */
public record PreparedTemporaryFile(
        UUID fileId,
        String filename,
        String contentType,
        long fileSize,
        String temporaryObjectKey,
        String durableObjectKey,
        String sourceEtag,
        Instant sourceLastModified
) {
    /** Compatibility constructor for callers/tests that have no source metadata yet. */
    public PreparedTemporaryFile(UUID fileId, String filename, String contentType,
                                 long fileSize, String durableObjectKey) {
        this(fileId, filename, contentType, fileSize, null, durableObjectKey, null, null);
    }
}
