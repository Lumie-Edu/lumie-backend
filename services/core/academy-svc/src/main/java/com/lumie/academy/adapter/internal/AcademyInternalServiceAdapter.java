package com.lumie.academy.adapter.internal;

import com.lumie.academy.application.dto.StudentResponse;
import com.lumie.academy.application.service.AcademyQueryService;
import com.lumie.academy.application.service.StudentQueryService;
import com.lumie.academy.domain.exception.AcademyNotFoundException;
import com.lumie.academy.domain.exception.StudentNotFoundException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.internal.academy.AcademyInternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AcademyInternalServiceAdapter implements AcademyInternalService {

    private final StudentQueryService studentQueryService;
    private final AcademyQueryService academyQueryService;

    @Override
    public Optional<StudentData> getStudent(String tenantSlug, Long studentId) {
        try {
            TenantContextHolder.setTenant(tenantSlug);
            StudentResponse student = studentQueryService.getStudent(studentId);
            return Optional.of(toStudentData(student));
        } catch (StudentNotFoundException e) {
            return Optional.empty();
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public StudentListData getStudentsByIds(String tenantSlug, List<Long> studentIds) {
        try {
            TenantContextHolder.setTenant(tenantSlug);
            List<StudentResponse> students = studentQueryService.getStudentsByIds(studentIds);
            return new StudentListData(
                    students.stream().map(this::toStudentData).toList(),
                    students.size(), 0, students.size()
            );
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public Optional<StudentData> getStudentByPhone(String tenantSlug, String phone) {
        try {
            TenantContextHolder.setTenant(tenantSlug);
            StudentResponse student = studentQueryService.getStudentByPhone(phone);
            return Optional.of(toStudentData(student));
        } catch (StudentNotFoundException e) {
            return Optional.empty();
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public StudentListData getStudentsByAcademy(String tenantSlug, Long academyId, int page, int size) {
        try {
            TenantContextHolder.setTenant(tenantSlug);
            Page<StudentResponse> students = studentQueryService.getActiveStudentsByAcademy(
                    academyId, PageRequest.of(page, size));
            return new StudentListData(
                    students.getContent().stream().map(this::toStudentData).toList(),
                    students.getTotalElements(), page, size
            );
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public ValidateStudentResult validateStudent(String tenantSlug, Long studentId) {
        try {
            TenantContextHolder.setTenant(tenantSlug);
            studentQueryService.getStudent(studentId);
            return new ValidateStudentResult(true, "Student is valid", studentId);
        } catch (StudentNotFoundException e) {
            return new ValidateStudentResult(false, "Student not found", studentId);
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public Optional<AcademyData> getAcademy(String tenantSlug, Long academyId) {
        try {
            TenantContextHolder.setTenant(tenantSlug);
            var academy = academyQueryService.getAcademy(academyId);
            return Optional.of(new AcademyData(
                    academy.id(), academy.name(),
                    academy.phone() != null ? academy.phone() : "",
                    academy.address() != null ? academy.address() : "",
                    academy.isActive(), academy.studentCount(),
                    academy.createdAt().toString(), academy.updatedAt().toString()
            ));
        } catch (AcademyNotFoundException e) {
            return Optional.empty();
        } finally {
            TenantContextHolder.clear();
        }
    }

    private StudentData toStudentData(StudentResponse dto) {
        return new StudentData(
                dto.id(), dto.userId(), dto.name(),
                dto.phone() != null ? dto.phone() : "",
                dto.academyId(), dto.academyName(),
                dto.isActive(),
                dto.createdAt().toString(), dto.updatedAt().toString()
        );
    }
}
