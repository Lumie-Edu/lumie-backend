package com.lumie.ai.application.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class ToolRegistry {

    private final Map<String, Tool> tools = new HashMap<>();

    public ToolRegistry(List<Tool> toolBeans) {
        for (Tool tool : toolBeans) {
            tools.put(tool.getName(), tool);
            log.info("Registered tool: {} [{}]", tool.getName(), tool.getCategory());
        }
    }

    public Optional<Tool> getTool(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public List<Map<String, Object>> getAllToolSchemas() {
        return tools.values().stream()
                .map(tool -> tool.getDefinition().toFunctionSchema())
                .toList();
    }

    public List<Tool> getAllTools() {
        return List.copyOf(tools.values());
    }
}
