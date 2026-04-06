package com.lumie.cls.application.dto;

import com.lumie.cls.domain.entity.ClassEnrollment;
import com.lumie.cls.domain.vo.EnrollmentStatus;

import java.time.LocalDateTime;

public record ClassEnrollmentResponse(
    Long id,
    Long classId,
    Long studentId,
    LocalDateTime enrolledAt,
    LocalDateTime droppedAt,
    EnrollmentStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static ClassEnrollmentResponse from(ClassEnrollment enrollment) {
        return new ClassEnrollmentResponse(
            enrollment.getId(),
            enrollment.getClassEntity().getId(),
            enrollment.getStudentId(),
            enrollment.getEnrolledAt(),
            enrollment.getDroppedAt(),
            enrollment.getStatus(),
            enrollment.getCreatedAt(),
            enrollment.getUpdatedAt()
        );
    }
}
