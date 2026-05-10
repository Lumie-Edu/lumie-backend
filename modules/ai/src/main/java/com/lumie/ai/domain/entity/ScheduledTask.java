package com.lumie.ai.domain.entity;

import com.lumie.ai.domain.vo.TaskFrequency;
import com.lumie.ai.domain.vo.TaskStatus;
import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "ai_scheduled_tasks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduledTask extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "tenant_slug", nullable = false, length = 100)
    private String tenantSlug;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "tool_name", nullable = false, length = 100)
    private String toolName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tool_arguments", columnDefinition = "jsonb")
    private String toolArguments;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 20)
    private TaskFrequency frequency;

    @Column(name = "cron_expression", length = 100)
    private String cronExpression;

    @Column(name = "next_execution_at")
    private Instant nextExecutionAt;

    @Column(name = "last_executed_at")
    private Instant lastExecutedAt;

    @Column(name = "last_result", columnDefinition = "TEXT")
    private String lastResult;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaskStatus status;

    @Builder
    public ScheduledTask(Long userId, String tenantSlug, String name, String description,
                         String toolName, String toolArguments, TaskFrequency frequency,
                         String cronExpression, Instant nextExecutionAt) {
        this.userId = userId;
        this.tenantSlug = tenantSlug;
        this.name = name;
        this.description = description;
        this.toolName = toolName;
        this.toolArguments = toolArguments;
        this.frequency = frequency;
        this.cronExpression = cronExpression;
        this.nextExecutionAt = nextExecutionAt;
        this.status = TaskStatus.ACTIVE;
    }

    public void pause() {
        this.status = TaskStatus.PAUSED;
    }

    public void resume() {
        this.status = TaskStatus.ACTIVE;
    }

    public void complete() {
        this.status = TaskStatus.COMPLETED;
    }

    public void fail() {
        this.status = TaskStatus.FAILED;
    }

    public void recordExecution(String result, Instant nextExecution) {
        this.lastExecutedAt = Instant.now();
        this.lastResult = result;
        this.nextExecutionAt = nextExecution;
    }
}
