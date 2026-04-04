package com.lumie.exam.adapter.out.internal;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.application.port.out.StudentServicePort;
import com.lumie.internal.academy.AcademyInternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component("examStudentInternalAdapter")
@RequiredArgsConstructor
public class StudentInternalAdapter implements StudentServicePort {

    private final AcademyInternalService academyInternalService;

    @Override
    public boolean existsById(Long studentId) {
        String slug = TenantContextHolder.getRequiredTenant();
        log.debug("Validating student via internal API: {} in tenant: {}", studentId, slug);
        return academyInternalService.validateStudent(slug, studentId).valid();
    }

    @Override
    public StudentInfo getStudentInfo(Long studentId) {
        String slug = TenantContextHolder.getRequiredTenant();
        log.debug("Getting student info via internal API: {} in tenant: {}", studentId, slug);
        return academyInternalService.getStudent(slug, studentId)
                .map(s -> new StudentInfo(s.id(), s.studentName(), "", s.studentPhone(), s.academyName(), s.isActive()))
                .orElse(null);
    }

    @Override
    public Map<Long, StudentInfo> getStudentInfoBatch(List<Long> studentIds) {
        String slug = TenantContextHolder.getRequiredTenant();
        log.debug("Batch getting student info via internal API for tenant: {}", slug);
        return academyInternalService.getStudentsByIds(slug, studentIds)
                .students()
                .stream()
                .collect(Collectors.toMap(
                        AcademyInternalService.StudentData::id,
                        s -> new StudentInfo(s.id(), s.studentName(), "", s.studentPhone(), s.academyName(), s.isActive())
                ));
    }

    @Override
    public Optional<StudentInfo> findByPhone(String phone) {
        String slug = TenantContextHolder.getRequiredTenant();
        log.debug("Finding student by phone via internal API in tenant: {}", slug);
        return academyInternalService.getStudentByPhone(slug, phone)
                .map(s -> new StudentInfo(s.id(), s.studentName(), "", s.studentPhone(), s.academyName(), s.isActive()));
    }
}
