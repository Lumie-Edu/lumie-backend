package com.lumie.auth.application.dto.response;

public record SessionResponse(
        String sid,
        String device,
        String deviceCategory,
        long createdAt,
        boolean isCurrent
) {
}
