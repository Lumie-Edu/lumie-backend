package com.lumie.ai.adapter.out.persistence;

import com.lumie.ai.domain.entity.ScheduledTask;
import com.lumie.ai.domain.repository.ScheduledTaskRepository;
import com.lumie.ai.domain.vo.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JpaScheduledTaskRepository extends JpaRepository<ScheduledTask, Long>, ScheduledTaskRepository {

    @Override
    Optional<ScheduledTask> findByIdAndUserId(Long id, Long userId);

    @Override
    List<ScheduledTask> findByUserId(Long userId);

    @Override
    List<ScheduledTask> findByStatusAndNextExecutionAtBefore(TaskStatus status, LocalDateTime time);
}
