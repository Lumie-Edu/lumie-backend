package com.lumie.academy.adapter.out.persistence;

import com.lumie.academy.domain.entity.Admin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JpaAdminRepository extends JpaRepository<Admin, Long> {

    @Query("SELECT a FROM Admin a WHERE a.user.id = :userId")
    Optional<Admin> findByUserId(@Param("userId") Long userId);

    @Query("SELECT a FROM Admin a WHERE a.user.email = :email")
    Optional<Admin> findByUserEmail(@Param("email") String email);

    @Query("SELECT a FROM Admin a WHERE a.academy.id = :academyId")
    Page<Admin> findByAcademyId(@Param("academyId") Long academyId, Pageable pageable);

    @Query("SELECT COUNT(a) FROM Admin a WHERE a.academy.id = :academyId")
    long countByAcademyId(@Param("academyId") Long academyId);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Admin a WHERE a.user.email = :email")
    boolean existsByUserEmail(@Param("email") String email);
}
