package com.lumie.file.application.dto.response;

import java.time.Instant;

public record FileDownloaderResponse(
        Long userId,
        String userName,
        Instant downloadedAt
) {
}
