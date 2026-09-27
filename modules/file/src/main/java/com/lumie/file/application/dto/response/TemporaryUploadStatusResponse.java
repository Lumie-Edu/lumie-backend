package com.lumie.file.application.dto.response;

import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.vo.FileLifecycleStatus;

import java.time.Instant;
import java.util.UUID;

public record TemporaryUploadStatusResponse(
        UUID fileId,
        boolean uploadCompleted,
        FileLifecycleStatus lifecycleStatus,
        Instant expiresAt
) {
    public static TemporaryUploadStatusResponse from(FileMetadata file) {
        return new TemporaryUploadStatusResponse(
                file.getId(), file.isUploadCompleted(), file.getLifecycleStatus(), file.getExpiresAt());
    }
}
