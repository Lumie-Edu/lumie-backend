package com.lumie.admin.application.dto;

import com.lumie.admin.domain.entity.Position;

import java.time.LocalDateTime;

public record PositionResponse(
    Long id,
    String name,
    Boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static PositionResponse from(Position position) {
        return new PositionResponse(
            position.getId(),
            position.getName(),
            position.isActive(),
            position.getCreatedAt(),
            position.getUpdatedAt()
        );
    }
}
