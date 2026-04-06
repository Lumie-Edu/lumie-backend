package com.lumie.journal.application.port.out;

import java.util.Optional;

public interface AcademyServicePort {

    Optional<AcademyInfo> getAcademy(Long academyId);

    record AcademyInfo(
        Long id,
        String name,
        boolean isActive
    ) {
    }
}
