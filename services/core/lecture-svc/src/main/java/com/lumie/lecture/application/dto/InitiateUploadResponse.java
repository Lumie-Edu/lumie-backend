package com.lumie.lecture.application.dto;

public record InitiateUploadResponse(
        Long lectureId,
        String presignedUrl,
        String objectKey
) {}
