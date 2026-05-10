package com.lumie.student.application.dto;

import com.lumie.student.domain.entity.Student;

import java.time.Instant;
import com.lumie.common.util.Timestamps;

public record StudentResponse(
    Long id,
    Long userId,
    String userLoginId,
    String name,
    String phone,
    String studentHighschool,
    Integer studentBirthYear,
    String parentPhone,
    String studentMemo,
    Boolean isActive,
    String avatarSeed,
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
            Timestamps.utcInstant(student.getCreatedAt()),
            Timestamps.utcInstant(student.getUpdatedAt())
        );
    }
}
