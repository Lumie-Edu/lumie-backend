package com.lumie.cls.application.dto;

import com.lumie.cls.domain.entity.ClassEnrollment;
import com.lumie.cls.domain.vo.EnrollmentStatus;

import java.time.Instant;
import com.lumie.common.util.Timestamps;

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
            Timestamps.utcInstant(enrollment.getEnrolledAt()),
            Timestamps.utcInstant(enrollment.getDroppedAt()),
            enrollment.getStatus(),
            Timestamps.utcInstant(enrollment.getCreatedAt()),
            Timestamps.utcInstant(enrollment.getUpdatedAt())
        );
    }
}
