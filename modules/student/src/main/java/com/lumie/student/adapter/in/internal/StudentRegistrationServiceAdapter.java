package com.lumie.student.adapter.in.internal;

import com.lumie.student.api.StudentRegistrationService;
import com.lumie.student.api.StudentService;
import com.lumie.student.application.dto.request.StudentRequest;
import com.lumie.student.application.dto.response.StudentResponse;
import com.lumie.student.application.service.StudentCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class StudentRegistrationServiceAdapter implements StudentRegistrationService {

    private final StudentCommandService studentCommandService;

    @Override
    @Transactional
    public StudentService.StudentData registerStudent(String tenantSlug, RegisterStudentCommand command) {
        StudentResponse registered = studentCommandService.registerStudent(new StudentRequest(
                command.name(), command.phone(), command.studentHighschool(), command.studentBirthYear(),
                command.parentPhone(), command.studentMemo()));
        return new StudentService.StudentData(
                registered.id(), registered.userId(), registered.name(), registered.phone(), registered.parentPhone(),
                Boolean.TRUE.equals(registered.isActive()),
                registered.createdAt() == null ? null : registered.createdAt().toString(),
                registered.updatedAt() == null ? null : registered.updatedAt().toString());
    }
}
