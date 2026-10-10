package com.lumie.classroom.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClassService {

    Optional<ClassData> getClass(String tenantSlug, Long classId);

    ClassListData getClassesByTeacher(String tenantSlug, Long teacherId, int page, int size);

    ClassListData getClassesByStudent(String tenantSlug, Long studentId, int page, int size);

    ClassListData getAllClasses(String tenantSlug, int page, int size);

    List<Long> getEnrolledStudentIds(String tenantSlug, Long classId);

    List<Long> getEnrolledStudentIds(String tenantSlug, Long tenantId, Long classId);

    List<Long> getActiveEnrolledStudentIds(String tenantSlug, Long tenantId);

    List<EnrolledClassData> getEnrolledClassesForStudents(String tenantSlug, Collection<Long> studentIds);

    boolean isStudentEnrolledInClass(String tenantSlug, Long studentId, Long classId);

    boolean hasActiveEnrollment(String tenantSlug, Long studentId);

    void dropActiveEnrollmentsForStudent(String tenantSlug, Long studentId);

    void replaceStudentEnrollments(String tenantSlug, Long studentId, Collection<Long> classIds);

    boolean hasAssignedClasses(String tenantSlug, Long staffId);

    record EnrolledClassData(Long classId, String className, Long studentId) {}

    record ClassData(
            Long id,
            String name,
            Long tenantId,
            Long teacherId,
            List<String> scheduleDays,
            String startTime,
            String endTime,
            String createdAt,
            String updatedAt
    ) {}

    record ClassListData(
            List<ClassData> classes,
            long totalCount,
            int page,
            int size
    ) {}
}
