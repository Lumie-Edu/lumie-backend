package com.lumie.activitylog.application.dto.response;

import com.lumie.activitylog.domain.entity.ActivityLog;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record ActivityLogResponse(
        Long id,
        Long actorId,
        String actorName,
        String actorRole,
        String action,
        String entityType,
        @Schema(nullable = true) Long entityId,
        @Schema(nullable = true) String entityName,
        @Schema(nullable = true) String description,
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
