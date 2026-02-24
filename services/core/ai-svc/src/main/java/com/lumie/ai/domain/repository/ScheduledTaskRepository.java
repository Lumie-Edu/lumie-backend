package com.lumie.ai.domain.repository;

import com.lumie.ai.domain.entity.ScheduledTask;
import com.lumie.ai.domain.vo.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScheduledTaskRepository {

    ScheduledTask save(ScheduledTask scheduledTask);

    Optional<ScheduledTask> findByIdAndUserId(Long id, Long userId);

    List<ScheduledTask> findByUserId(Long userId);

    List<ScheduledTask> findByStatusAndNextExecutionAtBefore(TaskStatus status, LocalDateTime time);

    void deleteById(Long id);
}
