package com.lumie.assignment.adapter.out.internal;

import com.lumie.assignment.application.port.out.ClassServicePort;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.internal.cls.ClassInternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component("assignmentClassInternalAdapter")
@RequiredArgsConstructor
public class ClassInternalAdapter implements ClassServicePort {

    private final ClassInternalService classInternalService;

    @Override
    public Optional<ClassInfo> getClass(Long classId) {
        String slug = TenantContextHolder.getRequiredTenant();
        log.debug("Getting class via internal API: {} in tenant: {}", classId, slug);
        return classInternalService.getClass(slug, classId)
                .map(c -> new ClassInfo(c.id(), c.name()));
    }
}
