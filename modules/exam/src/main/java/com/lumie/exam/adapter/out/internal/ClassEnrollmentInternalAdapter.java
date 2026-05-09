package com.lumie.exam.adapter.out.internal;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.application.port.out.ClassEnrollmentPort;
import com.lumie.internal.cls.ClassInternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClassEnrollmentInternalAdapter implements ClassEnrollmentPort {

    private final ClassInternalService classInternalService;

    @Override
    public List<ClassParticipant> findEnrolledClassesForStudents(Collection<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return List.of();
        }
        String slug = TenantContextHolder.getRequiredTenant();
        log.debug("Fetching enrolled classes for {} students in tenant: {}", studentIds.size(), slug);
        return classInternalService.getEnrolledClassesForStudents(slug, studentIds).stream()
                .map(d -> new ClassParticipant(d.classId(), d.className(), d.studentId()))
                .toList();
    }
}
