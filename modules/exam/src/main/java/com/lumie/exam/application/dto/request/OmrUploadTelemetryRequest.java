package com.lumie.exam.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Client-observed timings for one direct-to-MinIO OMR upload batch.
 *
 * <p>The payload deliberately excludes file names and user identifiers. Batch keys are emitted only
 * as structured log fields for correlation with the upload-proxy access log, never as metric labels.</p>
 */
public record OmrUploadTelemetryRequest(
        @NotBlank @Size(max = 64) String batchKey,
        @Min(1) @Max(100) int concurrency,
        @Min(0) long batchDurationMs,
        @Min(0) long totalBytes,
        @Size(max = 24) String connectionType,
        @NotEmpty @Size(max = 100) List<@Valid OmrUploadTelemetryFile> files
) {
    public record OmrUploadTelemetryFile(
            @Min(0) @Max(99) int imageIndex,
            @Min(0) long totalBytes,
            @Min(0) long uploadedBytes,
            @Min(0) long durationMs,
            @Min(0) @Max(599) int httpStatus,
            @NotNull OmrUploadOutcome outcome
    ) {}

    public enum OmrUploadOutcome {
        SUCCESS,
        HTTP_ERROR,
        NETWORK_ERROR,
        ABORTED
    }
}
