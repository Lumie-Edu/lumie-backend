package com.lumie.student.application.port.out;

import com.lumie.student.domain.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Application-owned persistence operations for students. */
public interface StudentPersistencePort {

    Student save(Student student);

    List<Student> saveAll(Iterable<Student> students);

    Optional<Student> findById(Long id);

    List<Student> findAllById(Iterable<Long> ids);

    Page<Student> findAll(Pageable pageable);

    Optional<Student> findByUserId(Long userId);

    List<Student> findByUserIdIn(Collection<Long> userIds);

    Optional<Student> findByUserLoginId(String userLoginId);

    Optional<Student> findByCanonicalPhone(String phone);

    Page<Student> findByIsActive(Boolean isActive, Pageable pageable);

    long countByIsActive(Boolean isActive);

    long countByCreatedAtBetween(Instant from, Instant to);

    long countByCreatedAtBefore(Instant cutoff);

    List<Object[]> countMonthlyEnrollmentsSince(Instant from);

    long countByIsActiveAndUpdatedAtBetween(Boolean isActive, Instant from, Instant to);

    long countByIsActiveAndUpdatedAtBefore(Boolean isActive, Instant cutoff);

    List<Object[]> countMonthlyDropoutsSince(Instant from);

    boolean existsByUserLoginId(String userLoginId);

    boolean existsByCanonicalPhone(String phone);

    boolean existsByCanonicalPhoneExcluding(String phone, Long excludeId);

    Page<Student> search(Boolean isActive, String search, Collection<Long> includedStudentIds,
                         Collection<Long> excludedStudentIds, Pageable pageable);
}
