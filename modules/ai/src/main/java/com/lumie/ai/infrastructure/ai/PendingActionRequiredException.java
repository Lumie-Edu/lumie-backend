package com.lumie.ai.infrastructure.ai;

import lombok.Getter;

import java.util.Map;

@Getter
public class PendingActionRequiredException extends RuntimeException {

    private final String toolName;
    private final String toolDescription;
    private final Map<String, Object> arguments;

    public PendingActionRequiredException(String toolName, String toolDescription, Map<String, Object> arguments) {
        super("Confirmation required for tool: " + toolName);
        this.toolName = toolName;
        this.toolDescription = toolDescription;
        this.arguments = arguments;
    }
}
