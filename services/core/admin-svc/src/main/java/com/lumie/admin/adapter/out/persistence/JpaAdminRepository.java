package com.lumie.admin.adapter.out.persistence;

import com.lumie.admin.domain.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface JpaAdminRepository extends JpaRepository<Admin, Long>, JpaSpecificationExecutor<Admin> {
    Optional<Admin> findByUserId(Long userId);
    Optional<Admin> findByUserLoginId(String userLoginId);
    boolean existsByUserLoginId(String userLoginId);
    boolean existsByPositionId(Long positionId);
}
