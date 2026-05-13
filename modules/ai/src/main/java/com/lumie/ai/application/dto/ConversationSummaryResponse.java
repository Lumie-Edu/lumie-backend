package com.lumie.ai.application.dto;

import java.time.Instant;

public record ConversationSummaryResponse(
        Long id,
        String title,
        Instant createdAt,
        Instant updatedAt
) {}
