package com.lumie.ai.application.dto.response;

import java.time.Instant;
import java.util.List;

public record ConversationDetailResponse(
        Long id,
        String title,
        boolean isActive,
        List<MessageResponse> messages,
        Instant createdAt,
        Instant updatedAt
) {}
