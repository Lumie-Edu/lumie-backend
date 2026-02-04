package com.lumie.exam.application.port.out;

import java.util.Optional;

public interface StudentServicePort {

    boolean existsById(Long studentId);

    StudentInfo getStudentInfo(Long studentId);

    Optional<StudentInfo> findByPhone(String phone);

    record StudentInfo(
            Long id,
            String name,
            String email,
            String phone
    ) {
    }
}
