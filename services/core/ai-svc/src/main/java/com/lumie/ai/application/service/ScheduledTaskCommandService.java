package com.lumie.ai.application.service;

import com.lumie.ai.domain.entity.ScheduledTask;
import com.lumie.ai.domain.exception.AiErrorCode;
import com.lumie.ai.domain.exception.AiException;
import com.lumie.ai.domain.repository.ScheduledTaskRepository;
import com.lumie.ai.domain.vo.TaskStatus;
import com.lumie.common.tenant.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ScheduledTaskCommandService {

    private final ScheduledTaskRepository scheduledTaskRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listTasks() {
        Long userId = UserContextHolder.getRequiredUserId();
        return scheduledTaskRepository.findByUserId(userId).stream()
                .map(this::toTaskResponse)
                .toList();
    }

    @Transactional
    public Map<String, Object> updateTask(Long taskId, Map<String, Object> updates) {
        Long userId = UserContextHolder.getRequiredUserId();
        ScheduledTask task = scheduledTaskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new AiException(AiErrorCode.SCHEDULED_TASK_NOT_FOUND));

        String status = (String) updates.get("status");
        if (status != null) {
            switch (TaskStatus.valueOf(status)) {
                case PAUSED -> task.pause();
                case ACTIVE -> task.resume();
                case COMPLETED -> task.complete();
                default -> throw new AiException(AiErrorCode.SCHEDULED_TASK_NOT_FOUND,
                        "Invalid status transition: " + status);
            }
        }

        ScheduledTask saved = scheduledTaskRepository.save(task);
        return toTaskResponse(saved);
    }

    @Transactional
    public void deleteTask(Long taskId) {
        Long userId = UserContextHolder.getRequiredUserId();
        scheduledTaskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new AiException(AiErrorCode.SCHEDULED_TASK_NOT_FOUND));
        scheduledTaskRepository.deleteById(taskId);
    }

    private Map<String, Object> toTaskResponse(ScheduledTask task) {
        return Map.of(
                "id", task.getId(),
                "name", task.getName(),
                "description", task.getDescription() != null ? task.getDescription() : "",
                "toolName", task.getToolName(),
                "frequency", task.getFrequency().name(),
                "status", task.getStatus().name(),
                "nextExecutionAt", task.getNextExecutionAt() != null ? task.getNextExecutionAt().toString() : "",
                "lastExecutedAt", task.getLastExecutedAt() != null ? task.getLastExecutedAt().toString() : "",
                "createdAt", task.getCreatedAt().toString()
        );
    }
}
