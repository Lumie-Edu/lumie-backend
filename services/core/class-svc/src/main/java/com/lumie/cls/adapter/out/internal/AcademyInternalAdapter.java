package com.lumie.cls.adapter.out.internal;

import com.lumie.cls.application.port.out.AcademyServicePort;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.internal.academy.AcademyInternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component("clsAcademyInternalAdapter")
@RequiredArgsConstructor
public class AcademyInternalAdapter implements AcademyServicePort {

    private final AcademyInternalService academyInternalService;

    @Override
    public Optional<AcademyInfo> getAcademy(Long academyId) {
        String slug = TenantContextHolder.getRequiredTenant();
        log.debug("Getting academy via internal API: {} in tenant: {}", academyId, slug);
        return academyInternalService.getAcademy(slug, academyId)
                .map(a -> new AcademyInfo(a.id(), a.academyName(), a.isActive()));
    }

    @Override
    public Optional<StudentInfo> getStudent(Long studentId) {
        String slug = TenantContextHolder.getRequiredTenant();
        log.debug("Getting student via internal API: {} in tenant: {}", studentId, slug);
        return academyInternalService.getStudent(slug, studentId)
                .map(s -> new StudentInfo(s.id(), s.studentName(), s.studentPhone(), s.academyId(), s.isActive()));
    }

    @Override
    public Map<Long, StudentInfo> getStudentsByIds(List<Long> studentIds) {
        String slug = TenantContextHolder.getRequiredTenant();
        log.debug("Batch getting students via internal API for tenant: {}", slug);
        return academyInternalService.getStudentsByIds(slug, studentIds)
                .students()
                .stream()
                .collect(Collectors.toMap(
                        AcademyInternalService.StudentData::id,
                        s -> new StudentInfo(s.id(), s.studentName(), s.studentPhone(), s.academyId(), s.isActive())
                ));
    }
}
