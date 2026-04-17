package com.lumie.internal.cls;

import java.util.List;
import java.util.Optional;

public interface ClassInternalService {

    Optional<ClassData> getClass(String tenantSlug, Long classId);

    ClassListData getClassesByAcademy(String tenantSlug, Long academyId, int page, int size);

    ClassListData getClassesByTeacher(String tenantSlug, Long teacherId, int page, int size);

    ClassListData getClassesByStudent(String tenantSlug, Long studentId, int page, int size);

    ClassListData getAllClasses(String tenantSlug, int page, int size);

    List<Long> getEnrolledStudentIds(String tenantSlug, Long classId);

    record ClassData(
            Long id,
            String name,
            Long academyId,
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
