package com.lumie.lecture.application.port.out;

public interface AcademyServicePort {

    boolean existsById(String tenantSlug, Long academyId);
}
