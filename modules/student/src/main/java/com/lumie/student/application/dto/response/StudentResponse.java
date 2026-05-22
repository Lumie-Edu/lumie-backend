package com.lumie.student.application.dto.response;

import com.lumie.student.domain.entity.Student;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record StudentResponse(
    Long id,
    Long userId,
    String userLoginId,
    String name,
    String phone,
    @Schema(nullable = true) String studentHighschool,
    @Schema(nullable = true) Integer studentBirthYear,
    String parentPhone,
    @Schema(nullable = true) String studentMemo,
    Boolean isActive,
    @Schema(nullable = true) String avatarSeed,
    Instant createdAt,
    Instant updatedAt
) {
    public static StudentResponse from(Student student) {
        return from(student, null);
    }

    public static StudentResponse from(Student student, String avatarSeed) {
        return new StudentResponse(
            student.getId(),
            student.getUserId(),
            student.getUserLoginId(),
            student.getStudentName(),
            student.getStudentPhone(),
            student.getStudentHighschool(),
            student.getStudentBirthYear(),
            student.getParentPhone(),
            student.getStudentMemo(),
            student.isActive(),
            avatarSeed,
            student.getCreatedAt(),
            student.getUpdatedAt()
        );
    }
}
