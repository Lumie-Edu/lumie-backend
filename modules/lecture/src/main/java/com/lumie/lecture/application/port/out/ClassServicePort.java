package com.lumie.lecture.application.port.out;

public interface ClassServicePort {

    boolean existsById(String tenantSlug, Long classId);

    boolean isStudentEnrolled(String tenantSlug, Long classId, Long studentId);
}
