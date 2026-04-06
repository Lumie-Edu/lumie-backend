package com.lumie.cls.application.port.out;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AcademyServicePort {

    Optional<AcademyInfo> getAcademy(Long academyId);

    Optional<StudentInfo> getStudent(Long studentId);

    Map<Long, StudentInfo> getStudentsByIds(List<Long> studentIds);

    record AcademyInfo(
        Long id,
        String name,
        boolean isActive
    ) {
    }

    record StudentInfo(
        Long id,
        String name,
        String phone,
        Long academyId,
        boolean isActive
    ) {
    }
}
