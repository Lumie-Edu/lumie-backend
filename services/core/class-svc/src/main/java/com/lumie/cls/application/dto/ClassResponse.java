package com.lumie.cls.application.dto;

import com.lumie.cls.domain.entity.ClassEntity;
import com.lumie.cls.domain.vo.ClassStatus;
import com.lumie.cls.domain.vo.SchedulePattern;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClassResponse(
    Long id,
    Long academyId,
    String name,
    String subject,
    Long teacherId,
    int maxStudents,
    long tuitionAmount,
    SchedulePattern schedulePattern,
    LocalDate startDate,
    LocalDate endDate,
    ClassStatus status,
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
            entity.getMaxStudents(),
            entity.getTuitionAmount(),
            entity.getSchedulePattern(),
            entity.getStartDate(),
            entity.getEndDate(),
            entity.getStatus(),
            entity.getDescription(),
            enrolledCount,
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
