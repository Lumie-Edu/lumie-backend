package com.lumie.activitylog.application.dto;

import com.lumie.activitylog.domain.entity.ActivityLog;

import java.time.Instant;

public record ActivityLogResponse(
        Long id,
        Long actorId,
        String actorName,
        String actorRole,
        String action,
        String entityType,
        Long entityId,
        String entityName,
        String description,
        Instant createdAt
) {
    public static ActivityLogResponse from(ActivityLog entity) {
        return new ActivityLogResponse(
                entity.getId(),
                entity.getActorId(),
                entity.getActorName(),
                entity.getActorRole().name(),
                entity.getAction().name(),
                entity.getEntityType().name(),
                entity.getEntityId(),
                entity.getEntityName(),
                entity.getDescription(),
                entity.getCreatedAt()
        );
    }
}
