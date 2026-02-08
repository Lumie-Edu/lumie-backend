package com.lumie.academy.domain.repository;

import com.lumie.academy.domain.entity.Admin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface AdminRepository {

    Admin save(Admin admin);

    Optional<Admin> findById(Long id);

    Optional<Admin> findByUserId(Long userId);

    Optional<Admin> findByUserLoginId(String userLoginId);

    Page<Admin> findByAcademiesId(Long academyId, Pageable pageable);

    Page<Admin> findAll(Pageable pageable);

    long countByAcademiesId(Long academyId);

    boolean existsByUserLoginId(String userLoginId);

    boolean existsByPositionId(Long positionId);

    void detach(Admin admin);
}
