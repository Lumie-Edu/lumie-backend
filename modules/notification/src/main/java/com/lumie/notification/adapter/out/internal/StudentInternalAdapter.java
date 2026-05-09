package com.lumie.notification.adapter.out.internal;

import com.lumie.notification.application.port.out.StudentServicePort;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.internal.student.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component("notificationStudentInternalAdapter")
@RequiredArgsConstructor
public class StudentInternalAdapter implements StudentServicePort {

    private final StudentService studentService;

    @Override
    public Optional<StudentInfo> getStudent(Long studentId) {
        String slug = TenantContextHolder.getRequiredTenant();
        log.debug("Getting student via internal API: {} in tenant: {}", studentId, slug);
        return studentService.getStudent(slug, studentId)
                .map(s -> new StudentInfo(s.id(), s.studentName(), s.studentPhone(), s.isActive()));
    }

    @Override
    public Map<Long, StudentInfo> getStudentsByIds(List<Long> studentIds) {
        String slug = TenantContextHolder.getRequiredTenant();
        log.debug("Batch getting students via internal API for tenant: {}", slug);
        return studentService.getStudentsByIds(slug, studentIds)
                .students()
                .stream()
                .collect(Collectors.toMap(
                        StudentService.StudentData::id,
                        s -> new StudentInfo(s.id(), s.studentName(), s.studentPhone(), s.isActive())
                ));
    }
}
