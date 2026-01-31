package com.lumie.content.application.port.out;

import java.util.Optional;

public interface StudentServicePort {

    boolean validateStudent(String tenantSlug, Long studentId);

    Optional<StudentData> getStudent(String tenantSlug, Long studentId);

    record StudentData(
            Long id,
            Long userId,
            String studentName,
            String studentPhone,
            Long academyId,
            String academyName,
            boolean isActive
    ) {
    }
}
