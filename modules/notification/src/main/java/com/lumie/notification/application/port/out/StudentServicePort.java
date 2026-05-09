package com.lumie.notification.application.port.out;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface StudentServicePort {

    Optional<StudentInfo> getStudent(Long studentId);

    Map<Long, StudentInfo> getStudentsByIds(List<Long> studentIds);

    record StudentInfo(
        Long id,
        String name,
        String phone,
        boolean isActive
    ) {
    }
}
