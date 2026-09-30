package com.lumie.common.auth;

import com.lumie.student.api.StudentService;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.ErrorCode;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import java.util.List;

public final class CurrentStudentResolver {
    private CurrentStudentResolver() {}

    public static Long requireStudentId(StudentService students, ErrorCode missingStudent) {
        return requireStudentId(students, TenantContextHolder.getRequiredTenant(),
                UserContextHolder.getRequiredUserId(), missingStudent);
    }

    public static Long requireStudentId(StudentService students, String tenantSlug,
                                        Long userId, ErrorCode missingStudent) {
        return students.getStudentsByUserIds(tenantSlug, List.of(userId)).students().stream()
                .findFirst()
                .map(StudentService.StudentData::id)
                .orElseThrow(() -> new BusinessException(missingStudent));
    }
}
