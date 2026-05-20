package com.lumie.ai.application.dto.response;

import java.time.Instant;

public record MessageResponse(
        Long id,
        String role,
        String content,
        String pendingAction,
        Instant createdAt
) {}
