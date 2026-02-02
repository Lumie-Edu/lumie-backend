package com.lumie.academy.domain.repository;

import com.lumie.academy.domain.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface StudentRepository {

    Student save(Student student);

    Optional<Student> findById(Long id);

    Optional<Student> findByUserId(Long userId);

    Optional<Student> findByUserLoginId(String userLoginId);

    Page<Student> findByAcademyId(Long academyId, Pageable pageable);

    Page<Student> findByAcademyIdAndIsActive(Long academyId, Boolean isActive, Pageable pageable);

    Page<Student> findAllByIsActive(Boolean isActive, Pageable pageable);

    Page<Student> findAll(Pageable pageable);

    long countByAcademyId(Long academyId);

    long countByIsActive(Boolean isActive);

    boolean existsByUserLoginId(String userLoginId);

    void delete(Student student);
}
