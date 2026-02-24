package com.lumie.ai.application.tool.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.ai.application.tool.*;
import com.lumie.ai.domain.entity.ScheduledTask;
import com.lumie.ai.domain.repository.ScheduledTaskRepository;
import com.lumie.ai.domain.vo.TaskFrequency;
import com.lumie.common.tenant.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleTaskTool implements Tool {

    private final ScheduledTaskRepository scheduledTaskRepository;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "schedule_task";
    }

    @Override
    public String getDescription() {
        return "Schedule a recurring or one-time task. For example, schedule daily attendance reports or weekly grade summaries.";
    }

    @Override
    public ToolDefinition getDefinition() {
        return new ToolDefinition(
                getName(),
                getDescription(),
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "name", Map.of(
                                        "type", "string",
                                        "description", "Task name"
                                ),
                                "description", Map.of(
                                        "type", "string",
                                        "description", "Task description"
                                ),
                                "toolName", Map.of(
                                        "type", "string",
                                        "description", "Name of the tool to execute (e.g., general_query, send_telegram)"
                                ),
                                "toolArguments", Map.of(
                                        "type", "object",
                                        "description", "Arguments to pass to the tool"
                                ),
                                "frequency", Map.of(
                                        "type", "string",
                                        "enum", List.of("ONCE", "DAILY", "WEEKLY", "MONTHLY"),
                                        "description", "How often to run the task"
                                ),
                                "cronExpression", Map.of(
                                        "type", "string",
                                        "description", "Cron expression for custom scheduling (optional, overrides frequency)"
                                )
                        ),
                        "required", List.of("name", "toolName", "toolArguments", "frequency")
                )
        );
    }

    @Override
    public ToolCategory getCategory() {
        return ToolCategory.WRITE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ToolResult execute(String tenantSlug, Map<String, Object> arguments) {
        try {
            String name = (String) arguments.get("name");
            String description = (String) arguments.get("description");
            String toolName = (String) arguments.get("toolName");
            Object toolArgs = arguments.get("toolArguments");
            String frequency = (String) arguments.get("frequency");
            String cronExpression = (String) arguments.get("cronExpression");

            String toolArgsJson = objectMapper.writeValueAsString(toolArgs);

            ScheduledTask task = ScheduledTask.builder()
                    .userId(UserContextHolder.getRequiredUserId())
                    .tenantSlug(tenantSlug)
                    .name(name)
                    .description(description)
                    .toolName(toolName)
                    .toolArguments(toolArgsJson)
                    .frequency(TaskFrequency.valueOf(frequency))
                    .cronExpression(cronExpression)
                    .nextExecutionAt(calculateNextExecution(TaskFrequency.valueOf(frequency)))
                    .build();

            ScheduledTask saved = scheduledTaskRepository.save(task);
            return ToolResult.success("Scheduled task created: " + saved.getName() + " (ID: " + saved.getId() + ")");
        } catch (Exception e) {
            log.error("Failed to create scheduled task: {}", e.getMessage());
            return ToolResult.failure("Failed to schedule task: " + e.getMessage());
        }
    }

    private LocalDateTime calculateNextExecution(TaskFrequency frequency) {
        LocalDateTime now = LocalDateTime.now();
        return switch (frequency) {
            case ONCE -> now.plusMinutes(1);
            case DAILY -> now.plusDays(1).withHour(9).withMinute(0).withSecond(0);
            case WEEKLY -> now.plusWeeks(1).withHour(9).withMinute(0).withSecond(0);
            case MONTHLY -> now.plusMonths(1).withDayOfMonth(1).withHour(9).withMinute(0).withSecond(0);
        };
    }
}
