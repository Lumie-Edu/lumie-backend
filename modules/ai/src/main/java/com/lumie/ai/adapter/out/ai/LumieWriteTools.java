package com.lumie.ai.adapter.out.ai;

import com.lumie.ai.domain.exception.PendingActionRequiredException;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class LumieWriteTools {

    @Tool(description = "Create a new announcement for the academy. Requires title and content.")
    public String createAnnouncement(
            @ToolParam(description = "Announcement title") String title,
            @ToolParam(description = "Announcement content body") String content,
            @ToolParam(description = "Whether this is an important announcement") boolean isImportant,
            @ToolParam(description = "Whether this is an asset announcement") boolean isAsset) {
        throw new PendingActionRequiredException("create_announcement",
                "Create a new announcement",
                Map.of("title", title, "content", content, "isImportant", isImportant, "isAsset", isAsset));
    }

    @Tool(description = "Send an SMS message to a phone number.")
    public String sendSms(
            @ToolParam(description = "Recipient phone number") String phoneNumber,
            @ToolParam(description = "SMS message content") String message) {
        throw new PendingActionRequiredException("send_sms",
                "Send an SMS message",
                Map.of("phoneNumber", phoneNumber, "message", message));
    }

    @Tool(description = "Send a Telegram message to a chat. Used for notifications and alerts.")
    public String sendTelegram(
            @ToolParam(description = "Telegram chat ID") String chatId,
            @ToolParam(description = "Message to send") String message) {
        throw new PendingActionRequiredException("send_telegram",
                "Send a Telegram message",
                Map.of("chatId", chatId, "message", message));
    }

    @Tool(description = "Schedule a recurring or one-time task, such as daily attendance reports or weekly grade summaries.")
    public String scheduleTask(
            @ToolParam(description = "Task name") String name,
            @ToolParam(description = "Task description") String description,
            @ToolParam(description = "Tool to execute (e.g., general_query, send_telegram)") String toolName,
            @ToolParam(description = "Arguments to pass to the tool as JSON string") String toolArguments,
            @ToolParam(description = "Frequency: ONCE, DAILY, WEEKLY, or MONTHLY") String frequency) {
        throw new PendingActionRequiredException("schedule_task",
                "Schedule a recurring task",
                Map.of("name", name, "description", description, "toolName", toolName,
                        "toolArguments", toolArguments, "frequency", frequency));
    }
}
