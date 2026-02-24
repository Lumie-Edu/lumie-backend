package com.lumie.ai.application.tool.impl;

import com.lumie.ai.application.port.out.TelegramNotificationPort;
import com.lumie.ai.application.tool.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendTelegramTool implements Tool {

    private final TelegramNotificationPort telegramNotificationPort;

    @Override
    public String getName() {
        return "send_telegram";
    }

    @Override
    public String getDescription() {
        return "Send a Telegram message to a chat. Used for notifications and alerts.";
    }

    @Override
    public ToolDefinition getDefinition() {
        return new ToolDefinition(
                getName(),
                getDescription(),
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "chatId", Map.of(
                                        "type", "string",
                                        "description", "Telegram chat ID"
                                ),
                                "message", Map.of(
                                        "type", "string",
                                        "description", "Message to send"
                                )
                        ),
                        "required", List.of("chatId", "message")
                )
        );
    }

    @Override
    public ToolCategory getCategory() {
        return ToolCategory.NOTIFICATION;
    }

    @Override
    public ToolResult execute(String tenantSlug, Map<String, Object> arguments) {
        String chatId = (String) arguments.get("chatId");
        String message = (String) arguments.get("message");

        try {
            telegramNotificationPort.send(chatId, message);
            return ToolResult.success("Telegram message sent to chat " + chatId);
        } catch (Exception e) {
            log.error("Failed to send Telegram message to {}: {}", chatId, e.getMessage());
            return ToolResult.failure("Failed to send Telegram message: " + e.getMessage());
        }
    }
}
