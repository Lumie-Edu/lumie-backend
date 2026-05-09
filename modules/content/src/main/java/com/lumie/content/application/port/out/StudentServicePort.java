package com.lumie.content.application.port.out;

import java.util.List;
import java.util.Optional;

public interface StudentServicePort {

    boolean validateStudent(String tenantSlug, Long studentId);

    Optional<StudentData> getStudent(String tenantSlug, Long studentId);

    List<StudentInfo> getStudentsByUserIds(String tenantSlug, List<Long> userIds);

    record StudentData(
            Long id,
            Long userId,
            String studentName,
            String studentPhone,
            boolean isActive
    ) {
    }

    record StudentInfo(
            Long userId,
            String name
    ) {
    }
}
