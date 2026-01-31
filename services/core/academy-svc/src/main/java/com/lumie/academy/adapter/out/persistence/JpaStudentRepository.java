package com.lumie.academy.adapter.out.persistence;

import com.lumie.academy.domain.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JpaStudentRepository extends JpaRepository<Student, Long> {

    @Query("SELECT s FROM Student s WHERE s.user.id = :userId")
    Optional<Student> findByUserId(@Param("userId") Long userId);

    @Query("SELECT s FROM Student s WHERE s.user.email = :email")
    Optional<Student> findByUserEmail(@Param("email") String email);

    @Query("SELECT s FROM Student s WHERE s.academy.id = :academyId")
    Page<Student> findByAcademyId(@Param("academyId") Long academyId, Pageable pageable);

    @Query("SELECT s FROM Student s WHERE s.academy.id = :academyId AND s.status = :status")
    Page<Student> findByAcademyIdAndStatus(@Param("academyId") Long academyId,
                                           @Param("status") String status,
                                           Pageable pageable);

    Page<Student> findByStatus(String status, Pageable pageable);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.academy.id = :academyId")
    long countByAcademyId(@Param("academyId") Long academyId);

    long countByStatus(String status);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Student s WHERE s.user.email = :email")
    boolean existsByUserEmail(@Param("email") String email);
}
