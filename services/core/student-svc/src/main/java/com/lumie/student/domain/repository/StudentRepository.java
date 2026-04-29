package com.lumie.student.domain.repository;

import com.lumie.student.domain.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StudentRepository {

    Student save(Student student);

    List<Student> saveAll(List<Student> students);

    Optional<Student> findById(Long id);

    List<Student> findAllByIds(List<Long> ids);

    Optional<Student> findByUserId(Long userId);

    List<Student> findAllByUserIds(List<Long> userIds);

    Optional<Student> findByUserLoginId(String userLoginId);

    Optional<Student> findByPhone(String phone);

    Page<Student> findAllByIsActive(Boolean isActive, Pageable pageable);

    Page<Student> findAll(Pageable pageable);

    Page<Student> search(Boolean isActive, String search, Pageable pageable);

    long countByIsActive(Boolean isActive);

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    long countByCreatedAtBefore(LocalDateTime cutoff);

    List<Object[]> countMonthlyEnrollmentsSince(LocalDateTime from);

    boolean existsByUserLoginId(String userLoginId);

    boolean existsByPhoneInAnyField(String phone);

    boolean existsByPhoneInAnyFieldExcluding(String phone, Long excludeId);

    void detach(Student student);
}
