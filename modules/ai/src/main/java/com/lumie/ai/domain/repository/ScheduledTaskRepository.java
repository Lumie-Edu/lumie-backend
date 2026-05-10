package com.lumie.ai.domain.repository;

import com.lumie.ai.domain.entity.ScheduledTask;
import com.lumie.ai.domain.vo.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ScheduledTaskRepository extends JpaRepository<ScheduledTask, Long> {

    Optional<ScheduledTask> findByIdAndUserId(Long id, Long userId);

    List<ScheduledTask> findByUserId(Long userId);

    List<ScheduledTask> findByStatusAndNextExecutionAtBefore(TaskStatus status, Instant time);
}
