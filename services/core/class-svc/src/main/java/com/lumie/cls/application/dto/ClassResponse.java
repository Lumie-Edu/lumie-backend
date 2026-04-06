package com.lumie.cls.application.dto;

import com.lumie.cls.domain.entity.ClassEntity;
import com.lumie.cls.domain.vo.SchedulePattern;

import java.time.LocalDateTime;

public record ClassResponse(
    Long id,
    Long academyId,
    String name,
    String subject,
    Long teacherId,
    SchedulePattern schedulePattern,
    String description,
    long enrolledCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static ClassResponse from(ClassEntity entity, long enrolledCount) {
        return new ClassResponse(
            entity.getId(),
            entity.getAcademyId(),
            entity.getName(),
            entity.getSubject(),
            entity.getTeacherId(),
            entity.getSchedulePattern(),
            entity.getDescription(),
            enrolledCount,
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
