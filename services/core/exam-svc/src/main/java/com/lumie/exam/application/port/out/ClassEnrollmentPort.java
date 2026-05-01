package com.lumie.exam.application.port.out;

import java.util.Collection;
import java.util.List;

public interface ClassEnrollmentPort {

    List<ClassParticipant> findEnrolledClassesForStudents(Collection<Long> studentIds);

    record ClassParticipant(Long classId, String className, Long studentId) {}
}
