package com.lumie.student.api;

/**
 * In-process command API for student creation. It is kept separate from
 * {@link StudentService} so read-only cross-module lookups do not depend on
 * the student command graph.
 */
public interface StudentRegistrationService {

    StudentService.StudentData registerStudent(String tenantSlug, RegisterStudentCommand command);

    record RegisterStudentCommand(
            String name,
            String phone,
            String studentHighschool,
            Integer studentBirthYear,
            String parentPhone,
            String studentMemo
    ) {}
}
