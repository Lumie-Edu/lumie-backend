package com.lumie.content.application.dto.request;

import java.util.List;
import java.util.UUID;

public record UpdateQnaRequest(
        String qnaTitle,
        String qnaContent,
        List<UUID> fileIdsToDelete
) {
}
