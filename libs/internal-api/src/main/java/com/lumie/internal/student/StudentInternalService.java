package com.lumie.internal.student;

import java.util.List;
import java.util.Optional;

/**
 * In-process internal API exposed by student-svc and consumed by other services
 * (e.g. exam-svc) for cross-module student lookups within the monolith.
 */
public interface StudentInternalService {

    Optional<StudentData> getStudent(String tenantSlug, Long studentId);

    StudentListData getStudentsByIds(String tenantSlug, List<Long> studentIds);

    Optional<StudentData> getStudentByPhone(String tenantSlug, String phone);

    StudentListData getStudentsByUserIds(String tenantSlug, List<Long> userIds);

    ValidateStudentResult validateStudent(String tenantSlug, Long studentId);

    record StudentData(
            Long id,
            Long userId,
            String studentName,
            String studentPhone,
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
}
