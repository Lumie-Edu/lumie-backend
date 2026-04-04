package com.lumie.content.adapter.out.internal;

import com.lumie.content.application.port.out.StudentServicePort;
import com.lumie.internal.academy.AcademyInternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class StudentInternalAdapter implements StudentServicePort {

    private final AcademyInternalService academyInternalService;

    @Override
    public boolean validateStudent(String tenantSlug, Long studentId) {
        log.debug("Validating student via internal API: {} in tenant: {}", studentId, tenantSlug);
        return academyInternalService.validateStudent(tenantSlug, studentId).valid();
    }

    @Override
    public Optional<StudentData> getStudent(String tenantSlug, Long studentId) {
        log.debug("Getting student via internal API: {} in tenant: {}", studentId, tenantSlug);
        return academyInternalService.getStudent(tenantSlug, studentId)
                .map(s -> new StudentData(
                        s.id(),
                        s.userId(),
                        s.studentName(),
                        s.studentPhone(),
                        s.academyId(),
                        s.academyName(),
                        s.isActive()
                ));
    }
}
