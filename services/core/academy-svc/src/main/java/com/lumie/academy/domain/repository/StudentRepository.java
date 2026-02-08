package com.lumie.academy.domain.repository;

import com.lumie.academy.domain.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface StudentRepository {

    Student save(Student student);

    List<Student> saveAll(List<Student> students);

    Optional<Student> findById(Long id);

    List<Student> findAllByIds(List<Long> ids);

    Optional<Student> findByUserId(Long userId);

    Optional<Student> findByUserLoginId(String userLoginId);

    Optional<Student> findByPhone(String phone);

    Page<Student> findByAcademyId(Long academyId, Pageable pageable);

    Page<Student> findByAcademyIdAndIsActive(Long academyId, Boolean isActive, Pageable pageable);

    Page<Student> findAllByIsActive(Boolean isActive, Pageable pageable);

    Page<Student> findAll(Pageable pageable);

    Page<Student> search(Long academyId, Boolean isActive, String search, String searchField, Pageable pageable);

    long countByAcademyId(Long academyId);

    long countByIsActive(Boolean isActive);

    boolean existsByUserLoginId(String userLoginId);

    void detach(Student student);
}
