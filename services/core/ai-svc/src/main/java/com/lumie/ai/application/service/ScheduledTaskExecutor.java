package com.lumie.ai.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.ai.application.tool.Tool;
import com.lumie.ai.application.tool.ToolRegistry;
import com.lumie.ai.application.tool.ToolResult;
import com.lumie.ai.domain.entity.ScheduledTask;
import com.lumie.ai.domain.repository.ScheduledTaskRepository;
import com.lumie.ai.domain.vo.TaskFrequency;
import com.lumie.ai.domain.vo.TaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTaskExecutor {

    private final ScheduledTaskRepository scheduledTaskRepository;
    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 60_000)
    public void executeDueTasks() {
        List<ScheduledTask> dueTasks = scheduledTaskRepository
                .findByStatusAndNextExecutionAtBefore(TaskStatus.ACTIVE, LocalDateTime.now());

        for (ScheduledTask task : dueTasks) {
            try {
                executeTask(task);
            } catch (Exception e) {
                log.error("Failed to execute scheduled task {}: {}", task.getId(), e.getMessage());
                task.fail();
                scheduledTaskRepository.save(task);
            }
        }
    }

    private void executeTask(ScheduledTask task) {
        log.info("Executing scheduled task: {} (ID: {})", task.getName(), task.getId());

        Tool tool = toolRegistry.getTool(task.getToolName())
                .orElseThrow(() -> new RuntimeException("Tool not found: " + task.getToolName()));

        Map<String, Object> arguments;
        try {
            arguments = objectMapper.readValue(task.getToolArguments(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse tool arguments", e);
        }

        ToolResult result = tool.execute(task.getTenantSlug(), arguments);

        LocalDateTime nextExecution = calculateNextExecution(task.getFrequency());

        if (task.getFrequency() == TaskFrequency.ONCE) {
            task.recordExecution(result.success() ? result.data() : result.error(), null);
            task.complete();
        } else {
            task.recordExecution(result.success() ? result.data() : result.error(), nextExecution);
        }

        scheduledTaskRepository.save(task);

        log.info("Scheduled task {} executed. Success: {}", task.getId(), result.success());
    }

    private LocalDateTime calculateNextExecution(TaskFrequency frequency) {
        LocalDateTime now = LocalDateTime.now();
        return switch (frequency) {
            case ONCE -> null;
            case DAILY -> now.plusDays(1);
            case WEEKLY -> now.plusWeeks(1);
            case MONTHLY -> now.plusMonths(1);
        };
    }
}
