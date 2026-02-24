package com.lumie.file.application.dto.response;

import java.io.InputStream;

public record FileDownloadResult(
        InputStream inputStream,
        String filename,
        String contentType,
        long fileSize
) {
}
