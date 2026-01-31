package com.lumie.academy.domain.repository;

import com.lumie.academy.domain.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface StudentRepository {

    Student save(Student student);

    Optional<Student> findById(Long id);

    Optional<Student> findByUserId(Long userId);

    Optional<Student> findByUserEmail(String email);

    Page<Student> findByAcademyId(Long academyId, Pageable pageable);

    Page<Student> findByAcademyIdAndStatus(Long academyId, String status, Pageable pageable);

    Page<Student> findAllByStatus(String status, Pageable pageable);

    long countByAcademyId(Long academyId);

    long countByStatus(String status);

    boolean existsByUserEmail(String email);

    void delete(Student student);
}
