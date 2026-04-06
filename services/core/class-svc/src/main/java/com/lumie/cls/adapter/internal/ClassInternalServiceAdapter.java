package com.lumie.cls.adapter.internal;

import com.lumie.cls.application.dto.ClassResponse;
import com.lumie.cls.application.service.ClassQueryService;
import com.lumie.cls.domain.exception.ClassNotFoundException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.internal.cls.ClassInternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClassInternalServiceAdapter implements ClassInternalService {

    private final ClassQueryService classQueryService;

    @Override
    public Optional<ClassData> getClass(String tenantSlug, Long classId) {
        String previous = TenantContextHolder.getTenant();
        try {
            TenantContextHolder.setTenant(tenantSlug);
            ClassResponse response = classQueryService.getClass(classId);
            return Optional.of(toClassData(response));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        } finally {
            restoreTenant(previous);
        }
    }

    @Override
    public ClassListData getClassesByAcademy(String tenantSlug, Long academyId, int page, int size) {
        String previous = TenantContextHolder.getTenant();
        try {
            TenantContextHolder.setTenant(tenantSlug);
            Page<ClassResponse> classes = classQueryService.listClasses(academyId, null, null,
                    PageRequest.of(page, size));
            return new ClassListData(
                    classes.getContent().stream().map(this::toClassData).toList(),
                    classes.getTotalElements(), page, size
            );
        } finally {
            restoreTenant(previous);
        }
    }

    @Override
    public ClassListData getClassesByTeacher(String tenantSlug, Long teacherId, int page, int size) {
        String previous = TenantContextHolder.getTenant();
        try {
            TenantContextHolder.setTenant(tenantSlug);
            Page<ClassResponse> classes = classQueryService.getClassesByTeacher(teacherId,
                    PageRequest.of(page, size));
            return new ClassListData(
                    classes.getContent().stream().map(this::toClassData).toList(),
                    classes.getTotalElements(), page, size
            );
        } finally {
            restoreTenant(previous);
        }
    }

    @Override
    public ClassListData getClassesByStudent(String tenantSlug, Long studentId, int page, int size) {
        String previous = TenantContextHolder.getTenant();
        try {
            TenantContextHolder.setTenant(tenantSlug);
            Page<ClassResponse> classes = classQueryService.getClassesByStudent(studentId,
                    PageRequest.of(page, size));
            return new ClassListData(
                    classes.getContent().stream().map(this::toClassData).toList(),
                    classes.getTotalElements(), page, size
            );
        } finally {
            restoreTenant(previous);
        }
    }

    private static void restoreTenant(String previous) {
        if (previous != null) {
            TenantContextHolder.setTenant(previous);
        } else {
            TenantContextHolder.clear();
        }
    }

    private ClassData toClassData(ClassResponse response) {
        return new ClassData(
                response.id(),
                response.name(),
                response.subject(),
                response.academyId(),
                response.teacherId(),
                response.enrolledCount(),
                response.createdAt() != null ? response.createdAt().toString() : "",
                response.updatedAt() != null ? response.updatedAt().toString() : ""
        );
    }
}
