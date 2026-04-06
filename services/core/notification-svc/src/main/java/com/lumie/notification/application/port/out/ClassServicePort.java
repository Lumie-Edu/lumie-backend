package com.lumie.notification.application.port.out;

import java.util.List;
import java.util.Optional;

public interface ClassServicePort {

    Optional<ClassInfo> getClass(Long classId);

    List<Long> getStudentIdsByClassId(Long classId);

    record ClassInfo(
        Long id,
        String name,
        Long academyId,
        long enrolledCount
    ) {
    }
}
