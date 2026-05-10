package com.lumie.activitylog.application.dto;

import com.lumie.activitylog.domain.entity.ActivityLog;

import java.time.Instant;
import com.lumie.common.util.Timestamps;

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
                entity.getActorRole(),
                entity.getAction(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getEntityName(),
                entity.getDescription(),
                Timestamps.utcInstant(entity.getCreatedAt())
        );
    }
}
