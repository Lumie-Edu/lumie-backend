package com.lumie.classroom.adapter.in.internal;

import com.lumie.classroom.application.dto.ClassResponse;
import com.lumie.classroom.application.service.ClassCommandService;
import com.lumie.classroom.application.service.ClassQueryService;
import com.lumie.classroom.domain.exception.ClassErrorCode;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.classroom.api.ClassService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClassServiceAdapter implements ClassService {

    private final ClassQueryService classQueryService;
    private final ClassCommandService classCommandService;

    @Override
    public Optional<ClassData> getClass(String tenantSlug, Long classId) {
        return TenantContextHolder.withinContext(tenantSlug, () -> {
            try {
                ClassResponse response = classQueryService.getClass(classId);
                return Optional.of(toClassData(response, TenantContextHolder.getTenantId()));
            } catch (BusinessException e) {
                if (e.getErrorCode() != ClassErrorCode.CLASS_NOT_FOUND) throw e;
                return Optional.<ClassData>empty();
            }
        });
    }

    @Override
    public ClassListData getClassesByTeacher(String tenantSlug, Long teacherId, int page, int size) {
        return TenantContextHolder.withinContext(tenantSlug, () -> {
            Page<ClassResponse> classes = classQueryService.getClassesByTeacher(teacherId,
                    PageRequest.of(page, size));
            Long tenantId = TenantContextHolder.getTenantId();
            return new ClassListData(
                    classes.getContent().stream().map(r -> toClassData(r, tenantId)).toList(),
                    classes.getTotalElements(), page, size
            );
        });
    }

    @Override
    public ClassListData getClassesByStudent(String tenantSlug, Long studentId, int page, int size) {
        return TenantContextHolder.withinContext(tenantSlug, () -> {
            Page<ClassResponse> classes = classQueryService.getClassesByStudent(studentId,
                    PageRequest.of(page, size));
            Long tenantId = TenantContextHolder.getTenantId();
            return new ClassListData(
                    classes.getContent().stream().map(r -> toClassData(r, tenantId)).toList(),
                    classes.getTotalElements(), page, size
            );
        });
    }

    @Override
    public ClassListData getAllClasses(String tenantSlug, int page, int size) {
        return TenantContextHolder.withinContext(tenantSlug, () -> {
            Page<ClassResponse> classes = classQueryService.listClasses(null, null,
                    PageRequest.of(page, size));
            Long tenantId = TenantContextHolder.getTenantId();
            return new ClassListData(
                    classes.getContent().stream().map(r -> toClassData(r, tenantId)).toList(),
                    classes.getTotalElements(), page, size
            );
        });
    }

    @Override
    public List<Long> getEnrolledStudentIds(String tenantSlug, Long classId) {
        return TenantContextHolder.withinContext(tenantSlug, () ->
                classQueryService.getEnrolledStudentIds(classId)
        );
    }

    @Override
    public List<Long> getEnrolledStudentIds(String tenantSlug, Long tenantId, Long classId) {
        return TenantContextHolder.withinContext(tenantSlug, tenantId, () ->
                classQueryService.getEnrolledStudentIds(classId)
        );
    }

    @Override
    public List<Long> getActiveEnrolledStudentIds(String tenantSlug, Long tenantId) {
        return TenantContextHolder.withinContext(tenantSlug, tenantId,
                classQueryService::getActiveEnrolledStudentIds);
    }

    @Override
    public List<EnrolledClassData> getEnrolledClassesForStudents(String tenantSlug, Collection<Long> studentIds) {
        return TenantContextHolder.withinContext(tenantSlug, () ->
                classQueryService.getEnrolledClassesForStudents(studentIds).stream()
                        .map(row -> new EnrolledClassData(row.classId(), row.className(), row.studentId()))
                        .toList()
        );
    }

    @Override
    public boolean isStudentEnrolledInClass(String tenantSlug, Long studentId, Long classId) {
        return TenantContextHolder.withinContext(tenantSlug, () ->
                classQueryService.isStudentEnrolledInClass(studentId, classId)
        );
    }

    @Override
    public boolean hasActiveEnrollment(String tenantSlug, Long studentId) {
        return TenantContextHolder.withinContext(tenantSlug, () ->
                classQueryService.hasActiveEnrollment(studentId)
        );
    }

    @Override
    public void dropActiveEnrollmentsForStudent(String tenantSlug, Long studentId) {
        TenantContextHolder.withinContext(tenantSlug, null,
                () -> classCommandService.dropActiveEnrollmentsForStudent(studentId));
    }

    @Override
    public void replaceStudentEnrollments(String tenantSlug, Long studentId, Collection<Long> classIds) {
        TenantContextHolder.withinContext(tenantSlug, null,
                () -> classCommandService.replaceStudentEnrollments(studentId, classIds));
    }

    @Override
    public boolean hasAssignedClasses(String tenantSlug, Long staffId) {
        return TenantContextHolder.withinContext(tenantSlug, () ->
                classQueryService.hasAssignedClasses(staffId)
        );
    }

    private ClassData toClassData(ClassResponse response, Long tenantId) {
        return new ClassData(
                response.id(),
                response.name(),
                tenantId,
                response.teacherId(),
                response.createdAt() != null ? response.createdAt().toString() : "",
                response.updatedAt() != null ? response.updatedAt().toString() : ""
        );
    }
}
