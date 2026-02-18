package com.lumie.admin.adapter.out.persistence;

import com.lumie.admin.domain.entity.Admin;
import com.lumie.admin.domain.repository.AdminRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AdminRepositoryAdapter implements AdminRepository {
    private final JpaAdminRepository jpaAdminRepository;
    private final EntityManager entityManager;

    @Override
    public Admin save(Admin admin) {
        return jpaAdminRepository.save(admin);
    }

    @Override
    public Optional<Admin> findById(Long id) {
        return jpaAdminRepository.findById(id);
    }

    @Override
    public Optional<Admin> findByUserId(Long userId) {
        return jpaAdminRepository.findByUserId(userId);
    }

    @Override
    public Optional<Admin> findByUserLoginId(String userLoginId) {
        return jpaAdminRepository.findByUserLoginId(userLoginId);
    }

    @Override
    public Page<Admin> findAll(Pageable pageable) {
        return jpaAdminRepository.findAll(pageable);
    }

    @Override
    public Page<Admin> findAll(Specification<Admin> spec, Pageable pageable) {
        return jpaAdminRepository.findAll(spec, pageable);
    }

    @Override
    public boolean existsByUserLoginId(String userLoginId) {
        return jpaAdminRepository.existsByUserLoginId(userLoginId);
    }

    @Override
    public boolean existsByPositionId(Long positionId) {
        return jpaAdminRepository.existsByPositionId(positionId);
    }

    @Override
    public boolean existsByPhone(String phone) {
        return jpaAdminRepository.existsByPhone(phone);
    }

    @Override
    public boolean existsByPhoneAndIdNot(String phone, Long excludeId) {
        return jpaAdminRepository.existsByPhoneAndIdNot(phone, excludeId);
    }

    @Override
    public void delete(Admin admin) {
        jpaAdminRepository.delete(admin);
    }

    @Override
    public void detach(Admin admin) {
        entityManager.detach(admin);
    }
}
