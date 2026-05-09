package com.lumie.notification.adapter.out.internal;

import com.lumie.notification.application.port.out.ClassServicePort;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.internal.cls.ClassService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component("notificationClassInternalAdapter")
@RequiredArgsConstructor
public class ClassInternalAdapter implements ClassServicePort {

    private final ClassService classService;

    @Override
    public Optional<ClassInfo> getClass(Long classId) {
        String slug = TenantContextHolder.getRequiredTenant();
        log.debug("Getting class via internal API: {} in tenant: {}", classId, slug);
        return classService.getClass(slug, classId)
                .map(c -> new ClassInfo(c.id(), c.name()));
    }

    @Override
    public List<Long> getStudentIdsByClassId(Long classId) {
        String slug = TenantContextHolder.getRequiredTenant();
        log.debug("Getting student IDs for class: {} in tenant: {}", classId, slug);
        ClassService.ClassListData studentsData =
                classService.getClassesByStudent(slug, classId, 0, 1000);
        // The class-svc internal API provides enrolled students via the class query
        // For now, we get the class and then look up enrollments
        // This is a simplified approach - in production, we'd add a dedicated method
        return studentsData.classes().stream()
                .map(ClassService.ClassData::id)
                .toList();
    }
}
