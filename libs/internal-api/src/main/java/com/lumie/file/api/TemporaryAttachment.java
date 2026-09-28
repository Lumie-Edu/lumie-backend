package com.lumie.file.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** Browser-uploaded temporary MinIO object. It has no persistent DB row. */
public record TemporaryAttachment(
        @NotBlank String objectKey,
        @NotBlank String filename,
        @NotBlank String contentType,
        @Positive long fileSize
) {}
