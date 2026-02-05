package com.lumie.academy.adapter.out.persistence;

import com.lumie.academy.domain.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JpaStudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {

    Optional<Student> findByUserId(Long userId);

    Optional<Student> findByUserLoginId(String userLoginId);

    Optional<Student> findByPhone(String phone);

    @Query("SELECT s FROM Student s WHERE s.academy.id = :academyId")
    Page<Student> findByAcademyId(@Param("academyId") Long academyId, Pageable pageable);

    @Query("SELECT s FROM Student s WHERE s.academy.id = :academyId AND s.isActive = :isActive")
    Page<Student> findByAcademyIdAndIsActive(@Param("academyId") Long academyId,
                                              @Param("isActive") Boolean isActive,
                                              Pageable pageable);

    Page<Student> findByIsActive(Boolean isActive, Pageable pageable);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.academy.id = :academyId")
    long countByAcademyId(@Param("academyId") Long academyId);

    long countByIsActive(Boolean isActive);

    boolean existsByUserLoginId(String userLoginId);
}
