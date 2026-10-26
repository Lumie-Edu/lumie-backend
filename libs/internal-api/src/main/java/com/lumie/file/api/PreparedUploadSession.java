package com.lumie.file.api;

import java.util.UUID;

/** Verified upload-session object, prepared outside the post-save DB transaction. */
public record PreparedUploadSession(
        UUID sessionId,
        String filename,
        String contentType,
        long fileSize,
        String objectKey,
        String etag
) {}
