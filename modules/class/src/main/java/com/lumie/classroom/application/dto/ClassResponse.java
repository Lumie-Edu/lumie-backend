package com.lumie.classroom.application.dto;

import com.lumie.classroom.domain.entity.ClassEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record ClassResponse(
    Long id,
    String name,
    @Schema(nullable = true) Long teacherId,
    @Schema(nullable = true) String teacherName,
    @Schema(nullable = true) SchedulePatternResponse schedulePattern,
    @Schema(nullable = true) String description,
    Instant createdAt,
    Instant updatedAt
) {
    public static ClassResponse from(ClassEntity entity, String teacherName) {
        return new ClassResponse(
            entity.getId(),
            entity.getName(),
            entity.getTeacherId(),
            teacherName,
            SchedulePatternResponse.from(entity.getSchedulePattern()),
            entity.getDescription(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
