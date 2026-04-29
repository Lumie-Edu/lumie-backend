package com.lumie.student.application.dto;

import com.lumie.student.domain.entity.Student;

import java.time.LocalDateTime;

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
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static StudentResponse from(Student student) {
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
            student.getCreatedAt(),
            student.getUpdatedAt()
        );
    }
}
