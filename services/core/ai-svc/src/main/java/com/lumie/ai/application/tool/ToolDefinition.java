package com.lumie.ai.application.tool;

import java.util.Map;

public record ToolDefinition(
        String name,
        String description,
        Map<String, Object> parameters
) {
    public Map<String, Object> toFunctionSchema() {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", name,
                        "description", description,
                        "parameters", parameters
                )
        );
    }
}
