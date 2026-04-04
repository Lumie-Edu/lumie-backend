package com.lumie.internal.academy;

import java.util.List;
import java.util.Optional;

public interface AcademyInternalService {

    Optional<StudentData> getStudent(String tenantSlug, Long studentId);

    StudentListData getStudentsByIds(String tenantSlug, List<Long> studentIds);

    Optional<StudentData> getStudentByPhone(String tenantSlug, String phone);

    StudentListData getStudentsByAcademy(String tenantSlug, Long academyId, int page, int size);

    ValidateStudentResult validateStudent(String tenantSlug, Long studentId);

    Optional<AcademyData> getAcademy(String tenantSlug, Long academyId);

    record StudentData(
            Long id,
            Long userId,
            String studentName,
            String studentPhone,
            Long academyId,
            String academyName,
            boolean isActive,
            String createdAt,
            String updatedAt
    ) {}

    record StudentListData(
            List<StudentData> students,
            long totalCount,
            int page,
            int size
    ) {}

    record ValidateStudentResult(
            boolean valid,
            String message,
            Long studentId
    ) {}

    record AcademyData(
            Long id,
            String academyName,
            String academyPhone,
            String academyAddress,
            boolean isActive,
            long studentCount,
            String createdAt,
            String updatedAt
    ) {}
}
