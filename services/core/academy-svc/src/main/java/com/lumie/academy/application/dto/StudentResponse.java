package com.lumie.academy.application.dto;

import com.lumie.academy.domain.entity.Student;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StudentResponse(
    Long id,
    Long userId,
    String email,
    String name,
    String phone,
    Long academyId,
    String academyName,
    String studentNumber,
    String grade,
    String schoolName,
    String parentName,
    String parentPhone,
    LocalDate enrollmentDate,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static StudentResponse from(Student student) {
        return new StudentResponse(
            student.getId(),
            student.getUserId(),
            student.getStudentEmail(),
            student.getStudentName(),
            student.getStudentPhone(),
            student.getAcademy().getId(),
            student.getAcademy().getName(),
            student.getStudentNumber(),
            student.getGrade(),
            student.getSchoolName(),
            student.getParentName(),
            student.getParentPhone(),
            student.getEnrollmentDate(),
            student.getStatus(),
            student.getCreatedAt(),
            student.getUpdatedAt()
        );
    }
}
