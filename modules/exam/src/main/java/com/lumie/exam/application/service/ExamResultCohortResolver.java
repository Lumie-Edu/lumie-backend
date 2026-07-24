package com.lumie.exam.application.service;

import com.lumie.classroom.api.ClassService;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.student.api.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Resolves the current student cohort used by exam-detail filters. */
@Service
@RequiredArgsConstructor
public class ExamResultCohortResolver {

    private final StudentService studentService;
    private final ClassService classService;

    public List<ExamResult> filter(
            List<ExamResult> results,
            Long classId,
            Boolean hasActiveEnrollment,
            Boolean isActive) {
        if (classId == null && hasActiveEnrollment == null && isActive == null) {
            return results;
        }

        String tenantSlug = TenantContextHolder.getRequiredTenant();
        Map<Long, StudentService.StudentData> studentsById = loadStudents(tenantSlug, results);
        Set<Long> enrollmentStudentIds = loadEnrollmentStudentIds(
                tenantSlug, classId, hasActiveEnrollment);

        return results.stream()
                .filter(result -> matches(
                        result,
                        studentsById,
                        classId,
                        hasActiveEnrollment,
                        isActive,
                        enrollmentStudentIds))
                .toList();
    }

    private Map<Long, StudentService.StudentData> loadStudents(
            String tenantSlug,
            List<ExamResult> results) {
        List<Long> studentIds = results.stream()
                .map(ExamResult::getStudentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (studentIds.isEmpty()) {
            return Map.of();
        }
        return studentService.getStudentsByIds(tenantSlug, studentIds).students().stream()
                .collect(Collectors.toMap(StudentService.StudentData::id, student -> student));
    }

    private Set<Long> loadEnrollmentStudentIds(
            String tenantSlug,
            Long classId,
            Boolean hasActiveEnrollment) {
        if (classId == null && hasActiveEnrollment == null) {
            return Set.of();
        }

        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<Long> studentIds = classId != null
                ? classService.getEnrolledStudentIds(tenantSlug, tenantId, classId)
                : classService.getActiveEnrolledStudentIds(tenantSlug, tenantId);
        return Set.copyOf(studentIds);
    }

    private static boolean matches(
            ExamResult result,
            Map<Long, StudentService.StudentData> studentsById,
            Long classId,
            Boolean hasActiveEnrollment,
            Boolean isActive,
            Set<Long> enrollmentStudentIds) {
        Long studentId = result.getStudentId();
        StudentService.StudentData student = studentId == null ? null : studentsById.get(studentId);
        boolean registered = !result.isStudentDeleted() && student != null;

        if (classId != null && (!registered || !enrollmentStudentIds.contains(studentId))) {
            return false;
        }

        if (classId == null && hasActiveEnrollment != null) {
            boolean enrolled = registered && enrollmentStudentIds.contains(studentId);
            if (hasActiveEnrollment != enrolled) {
                return false;
            }
        }

        return isActive == null || (registered && isActive == student.isActive());
    }
}
