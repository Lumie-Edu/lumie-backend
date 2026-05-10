package com.lumie.cls.application.dto;

import com.lumie.cls.domain.entity.ClassEntity;
import com.lumie.cls.domain.vo.SchedulePattern;

import java.time.Instant;
import com.lumie.common.util.Timestamps;

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
            Timestamps.utcInstant(entity.getCreatedAt()),
            Timestamps.utcInstant(entity.getUpdatedAt())
        );
    }
}
