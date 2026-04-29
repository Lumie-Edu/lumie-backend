package com.lumie.content.adapter.out.internal;

import com.lumie.content.application.port.out.StudentServicePort;
import com.lumie.internal.student.StudentInternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component("contentStudentInternalAdapter")
@RequiredArgsConstructor
public class StudentInternalAdapter implements StudentServicePort {

    private final StudentInternalService studentInternalService;

    @Override
    public boolean validateStudent(String tenantSlug, Long studentId) {
        log.debug("Validating student via internal API: {} in tenant: {}", studentId, tenantSlug);
        return studentInternalService.validateStudent(tenantSlug, studentId).valid();
    }

    @Override
    public Optional<StudentData> getStudent(String tenantSlug, Long studentId) {
        log.debug("Getting student via internal API: {} in tenant: {}", studentId, tenantSlug);
        return studentInternalService.getStudent(tenantSlug, studentId)
                .map(s -> new StudentData(
                        s.id(),
                        s.userId(),
                        s.studentName(),
                        s.studentPhone(),
                        s.isActive()
                ));
    }

    @Override
    public List<StudentInfo> getStudentsByUserIds(String tenantSlug, List<Long> userIds) {
        log.debug("Getting students by userIds via internal API, tenant: {}, userIds: {}", tenantSlug, userIds);
        return studentInternalService.getStudentsByUserIds(tenantSlug, userIds)
                .students()
                .stream()
                .map(s -> new StudentInfo(s.userId(), s.studentName()))
                .toList();
    }

}
