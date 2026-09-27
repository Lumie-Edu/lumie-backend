package com.lumie.file.api;

import java.util.UUID;

/** Object already copied out of tmp storage, awaiting a short DB attachment transaction. */
public record PreparedTemporaryFile(
        UUID fileId,
        String filename,
        String contentType,
        long fileSize,
        String durableObjectKey
) {}
