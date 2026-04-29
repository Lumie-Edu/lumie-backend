package com.lumie.student.adapter.out.persistence;

import com.lumie.student.domain.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JpaStudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {

    Optional<Student> findByUserId(Long userId);

    List<Student> findByUserIdIn(List<Long> userIds);

    Optional<Student> findByUserLoginId(String userLoginId);

    Optional<Student> findByPhone(String phone);

    Page<Student> findByIsActive(Boolean isActive, Pageable pageable);

    long countByIsActive(Boolean isActive);

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    boolean existsByUserLoginId(String userLoginId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Student s WHERE s.phone = :phone OR s.parentPhone = :phone")
    boolean existsByPhoneInAnyField(@Param("phone") String phone);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Student s WHERE (s.phone = :phone OR s.parentPhone = :phone) AND s.id != :excludeId")
    boolean existsByPhoneInAnyFieldExcluding(@Param("phone") String phone, @Param("excludeId") Long excludeId);
}
