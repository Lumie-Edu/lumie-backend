package com.lumie.student.adapter.in.internal;

import com.lumie.student.api.StudentService;
import com.lumie.student.domain.entity.Student;
import com.lumie.student.domain.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * In-process implementation of {@link StudentService} exposed by
 * student-svc. Other modules in the monolith inject this Spring bean to look
 * up Student data without going through the public REST API.
 *
 * Tenant scoping is enforced upstream via {@code TenantContextHolder}; the
 * {@code tenantSlug} arg is currently informational and kept on the contract
 * for symmetry with other internal-service contracts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentServiceAdapter implements StudentService {

    private final StudentRepository studentRepository;

    @Override
    public Optional<StudentData> getStudent(String tenantSlug, Long studentId) {
        log.debug("internal.getStudent slug={} id={}", tenantSlug, studentId);
        return studentRepository.findById(studentId).map(this::toData);
    }

    @Override
    public StudentListData getStudentsByIds(String tenantSlug, List<Long> studentIds) {
        log.debug("internal.getStudentsByIds slug={} count={}", tenantSlug,
                studentIds == null ? 0 : studentIds.size());
        if (studentIds == null || studentIds.isEmpty()) {
            return new StudentListData(List.of(), 0L, 0, 0);
        }
        List<StudentData> students = studentRepository.findAllById(studentIds).stream()
                .map(this::toData)
                .toList();
        return new StudentListData(students, students.size(), 0, students.size());
    }

    @Override
    public Optional<StudentData> getStudentByPhone(String tenantSlug, String phone) {
        log.debug("internal.getStudentByPhone slug={}", tenantSlug);
        return studentRepository.findByPhone(phone).map(this::toData);
    }

    @Override
    public StudentListData getStudentsByUserIds(String tenantSlug, List<Long> userIds) {
        log.debug("internal.getStudentsByUserIds slug={} count={}", tenantSlug,
                userIds == null ? 0 : userIds.size());
        if (userIds == null || userIds.isEmpty()) {
            return new StudentListData(List.of(), 0L, 0, 0);
        }
        List<StudentData> students = studentRepository.findByUserIdIn(userIds).stream()
                .map(this::toData)
                .toList();
        return new StudentListData(students, students.size(), 0, students.size());
    }

    @Override
    public ValidateStudentResult validateStudent(String tenantSlug, Long studentId) {
        log.debug("internal.validateStudent slug={} id={}", tenantSlug, studentId);
        return studentRepository.findById(studentId)
                .map(s -> new ValidateStudentResult(s.isActive(),
                        s.isActive() ? "OK" : "INACTIVE", studentId))
                .orElseGet(() -> new ValidateStudentResult(false, "NOT_FOUND", studentId));
    }

    @Override
    public List<Long> searchUserIds(String tenantSlug, String keyword) {
        log.debug("internal.searchUserIds slug={} keyword={}", tenantSlug, keyword);
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return studentRepository.search(null, keyword, org.springframework.data.domain.PageRequest.of(0, 1000))
                .stream()
                .map(Student::getUserId)
                .filter(id -> id != null)
                .toList();
    }

    private StudentData toData(Student s) {
        return new StudentData(
                s.getId(),
                s.getUserId(),
                s.getStudentName(),
                s.getStudentPhone(),
                s.isActive(),
                formatTs(s.getCreatedAt()),
                formatTs(s.getUpdatedAt())
        );
    }

    private static String formatTs(Instant ts) {
        return ts == null ? null : ts.toString();
    }
}
