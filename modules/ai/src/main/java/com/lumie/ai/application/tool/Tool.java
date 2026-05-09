package com.lumie.ai.application.tool;

import java.util.Map;

public interface Tool {

    String getName();

    String getDescription();

    ToolDefinition getDefinition();

    ToolCategory getCategory();

    ToolResult execute(String tenantSlug, Map<String, Object> arguments);
}
