package com.lumie.exam.application.dto.response;

import java.util.List;

public record OmrPresignResponse(
        String batchKey,
        List<PresignedUpload> uploads
) {
    public record PresignedUpload(
            String fileName,
            String objectKey,
            String uploadUrl
    ) {}
}
