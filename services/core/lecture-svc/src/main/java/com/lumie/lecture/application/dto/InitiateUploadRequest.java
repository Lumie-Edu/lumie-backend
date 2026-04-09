package com.lumie.lecture.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InitiateUploadRequest(
        @NotNull Long lectureId,
        @NotBlank String filename,
        @NotBlank String contentType,
        @NotNull @Positive Long fileSize
) {}
