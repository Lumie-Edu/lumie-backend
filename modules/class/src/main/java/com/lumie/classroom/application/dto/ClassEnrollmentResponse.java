package com.lumie.classroom.application.dto;

import com.lumie.classroom.domain.entity.ClassEnrollment;
import com.lumie.classroom.domain.vo.EnrollmentStatus;

import java.time.Instant;

public record ClassEnrollmentResponse(
    Long id,
    Long classId,
    Long studentId,
    Instant enrolledAt,
    Instant droppedAt,
    EnrollmentStatus status,
    Instant createdAt,
    Instant updatedAt
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
