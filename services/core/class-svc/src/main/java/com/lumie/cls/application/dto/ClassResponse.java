package com.lumie.cls.application.dto;

import com.lumie.cls.domain.entity.ClassEntity;
import com.lumie.cls.domain.vo.SchedulePattern;

import java.time.LocalDateTime;

public record ClassResponse(
    Long id,
    Long academyId,
    String name,
    Long teacherId,
    String teacherName,
    SchedulePattern schedulePattern,
    String description,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static ClassResponse from(ClassEntity entity, String teacherName) {
        return new ClassResponse(
            entity.getId(),
            entity.getAcademyId(),
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
