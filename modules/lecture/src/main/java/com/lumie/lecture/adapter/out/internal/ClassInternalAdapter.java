package com.lumie.lecture.adapter.out.internal;

import com.lumie.internal.cls.ClassInternalService;
import com.lumie.lecture.application.port.out.ClassServicePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClassInternalAdapter implements ClassServicePort {

    private final ClassInternalService classInternalService;

    @Override
    public boolean existsById(String tenantSlug, Long classId) {
        return classInternalService.getClass(tenantSlug, classId).isPresent();
    }

    @Override
    public boolean isStudentEnrolled(String tenantSlug, Long classId, Long studentId) {
        return classInternalService.getClassesByStudent(tenantSlug, studentId, 0, 1000)
                .classes().stream()
                .anyMatch(c -> c.id().equals(classId));
    }
}
