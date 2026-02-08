package com.lumie.academy.application.dto;

import com.lumie.academy.domain.entity.Student;

import java.time.LocalDateTime;

public record StudentResponse(
    Long id,
    Long userId,
    String userLoginId,
    String name,
    String phone,
    Long academyId,
    String academyName,
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
            student.getAcademy().getId(),
            student.getAcademy().getName(),
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
