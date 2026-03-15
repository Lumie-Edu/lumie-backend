package com.lumie.exam.application.port.out;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface StudentServicePort {

    boolean existsById(Long studentId);

    StudentInfo getStudentInfo(Long studentId);

    Map<Long, StudentInfo> getStudentInfoBatch(List<Long> studentIds);

    Optional<StudentInfo> findByPhone(String phone);

    record StudentInfo(
            Long id,
            String name,
            String email,
            String phone,
            String academyName,
            boolean isActive
    ) {
    }
}
