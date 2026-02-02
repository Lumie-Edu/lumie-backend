package com.lumie.academy.adapter.out.persistence;

import com.lumie.academy.domain.entity.Admin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JpaAdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByUserId(Long userId);

    Optional<Admin> findByUserLoginId(String userLoginId);

    @Query("SELECT DISTINCT a FROM Admin a JOIN a.academies ac WHERE ac.id = :academyId")
    Page<Admin> findByAcademiesId(@Param("academyId") Long academyId, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT a) FROM Admin a JOIN a.academies ac WHERE ac.id = :academyId")
    long countByAcademiesId(@Param("academyId") Long academyId);

    boolean existsByUserLoginId(String userLoginId);
}
