package com.lumie.classroom.application.dto;

import com.lumie.classroom.domain.entity.ClassEntity;
import com.lumie.classroom.domain.vo.SchedulePattern;

import java.time.Instant;

public record ClassResponse(
    Long id,
    String name,
    Long teacherId,
    String teacherName,
    SchedulePattern schedulePattern,
    String description,
    Instant createdAt,
    Instant updatedAt
) {
    public static ClassResponse from(ClassEntity entity, String teacherName) {
        return new ClassResponse(
            entity.getId(),
            entity.getName(),
            entity.getTeacherId(),
            teacherName,
            entity.getSchedulePattern(),
            entity.getDescription(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
