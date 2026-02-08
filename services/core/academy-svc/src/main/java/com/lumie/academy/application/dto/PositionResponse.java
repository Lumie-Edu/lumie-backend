package com.lumie.academy.application.dto;

import com.lumie.academy.domain.entity.Position;

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
