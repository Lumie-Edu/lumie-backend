package com.lumie.exam.application.dto.response;

import java.time.Instant;

public record AiCommentaryResponse(
        String content,
        boolean isEdited,
        Instant generatedAt,
        Instant editedAt,
        Long version
) {
    public static AiCommentaryResponse empty() {
        return new AiCommentaryResponse(null, false, null, null, null);
    }
}
