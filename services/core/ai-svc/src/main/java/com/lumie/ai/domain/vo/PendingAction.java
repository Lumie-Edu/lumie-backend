package com.lumie.ai.domain.vo;

import java.util.Map;

public record PendingAction(
        String actionId,
        String toolName,
        String description,
        Map<String, Object> arguments
) {
}
