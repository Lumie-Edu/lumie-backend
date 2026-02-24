package com.lumie.ai.application.tool.impl;

import com.lumie.ai.application.port.out.SmsNotificationPort;
import com.lumie.ai.application.tool.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendSmsTool implements Tool {

    private final SmsNotificationPort smsNotificationPort;

    @Override
    public String getName() {
        return "send_sms";
    }

    @Override
    public String getDescription() {
        return "Send an SMS message to a phone number. Currently a stub implementation - will log the message but not actually send.";
    }

    @Override
    public ToolDefinition getDefinition() {
        return new ToolDefinition(
                getName(),
                getDescription(),
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "phoneNumber", Map.of(
                                        "type", "string",
                                        "description", "Recipient phone number"
                                ),
                                "message", Map.of(
                                        "type", "string",
                                        "description", "SMS message content"
                                )
                        ),
                        "required", List.of("phoneNumber", "message")
                )
        );
    }

    @Override
    public ToolCategory getCategory() {
        return ToolCategory.NOTIFICATION;
    }

    @Override
    public ToolResult execute(String tenantSlug, Map<String, Object> arguments) {
        String phoneNumber = (String) arguments.get("phoneNumber");
        String message = (String) arguments.get("message");

        try {
            smsNotificationPort.send(phoneNumber, message);
            return ToolResult.success("SMS sent to " + phoneNumber);
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage());
            return ToolResult.failure("Failed to send SMS: " + e.getMessage());
        }
    }
}
