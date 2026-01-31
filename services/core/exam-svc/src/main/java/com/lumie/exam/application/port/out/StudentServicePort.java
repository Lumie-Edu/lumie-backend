package com.lumie.exam.application.port.out;

public interface StudentServicePort {

    boolean existsById(Long studentId);

    StudentInfo getStudentInfo(Long studentId);

    record StudentInfo(
            Long id,
            String name,
            String email,
            String phone
    ) {
    }
}
