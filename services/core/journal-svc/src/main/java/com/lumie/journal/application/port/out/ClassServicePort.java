package com.lumie.journal.application.port.out;

import java.util.Optional;

public interface ClassServicePort {

    Optional<ClassInfo> getClass(Long classId);

    record ClassInfo(
        Long id,
        String name,
        Long academyId
    ) {
    }
}
