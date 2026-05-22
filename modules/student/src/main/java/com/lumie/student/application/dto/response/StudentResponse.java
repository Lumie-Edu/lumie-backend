package com.lumie.student.application.dto.response;

import com.lumie.student.domain.entity.Student;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record StudentResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long userId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String userLoginId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String phone,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String studentHighschool,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Integer studentBirthYear,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String parentPhone,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String studentMemo,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Boolean isActive,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String avatarSeed,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant updatedAt
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
