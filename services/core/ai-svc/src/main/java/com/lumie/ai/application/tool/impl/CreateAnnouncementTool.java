package com.lumie.ai.application.tool.impl;

import com.lumie.ai.application.port.out.ContentServicePort;
import com.lumie.ai.application.tool.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateAnnouncementTool implements Tool {

    private final ContentServicePort contentServicePort;

    @Override
    public String getName() {
        return "create_announcement";
    }

    @Override
    public String getDescription() {
        return "Create a new announcement for the academy. Requires title and content. Optionally mark as important or asset announcement.";
    }

    @Override
    public ToolDefinition getDefinition() {
        return new ToolDefinition(
                getName(),
                getDescription(),
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "title", Map.of(
                                        "type", "string",
                                        "description", "Announcement title"
                                ),
                                "content", Map.of(
                                        "type", "string",
                                        "description", "Announcement content body"
                                ),
                                "isImportant", Map.of(
                                        "type", "boolean",
                                        "description", "Whether this is an important announcement"
                                ),
                                "isAsset", Map.of(
                                        "type", "boolean",
                                        "description", "Whether this is an asset announcement"
                                )
                        ),
                        "required", List.of("title", "content")
                )
        );
    }

    @Override
    public ToolCategory getCategory() {
        return ToolCategory.WRITE;
    }

    @Override
    public ToolResult execute(String tenantSlug, Map<String, Object> arguments) {
        try {
            String result = contentServicePort.createAnnouncement(arguments);
            return ToolResult.success(result);
        } catch (Exception e) {
            log.error("Failed to create announcement for tenant {}: {}", tenantSlug, e.getMessage());
            return ToolResult.failure("Failed to create announcement: " + e.getMessage());
        }
    }
}
