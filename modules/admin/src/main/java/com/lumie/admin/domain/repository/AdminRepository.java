package com.lumie.admin.domain.repository;

import com.lumie.admin.domain.entity.Admin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

public interface AdminRepository {
    Admin save(Admin admin);
    Optional<Admin> findById(Long id);
    Optional<Admin> findByUserId(Long userId);
    Optional<Admin> findByUserLoginId(String userLoginId);
    Page<Admin> findAll(Pageable pageable);
    Page<Admin> findAll(Specification<Admin> spec, Pageable pageable);
    boolean existsByUserLoginId(String userLoginId);
    boolean existsByPhone(String phone);
    boolean existsByPhoneAndIdNot(String phone, Long excludeId);
    long countByIsActive(Boolean isActive);
    List<Object[]> countActiveGroupedByRole();
    void delete(Admin admin);
    void detach(Admin admin);
}
