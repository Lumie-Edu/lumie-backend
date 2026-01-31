package com.lumie.academy.adapter.out.persistence;

import com.lumie.academy.domain.entity.Admin;
import com.lumie.academy.domain.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AdminRepositoryAdapter implements AdminRepository {

    private final JpaAdminRepository jpaAdminRepository;

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
    public Optional<Admin> findByUserEmail(String email) {
        return jpaAdminRepository.findByUserEmail(email);
    }

    @Override
    public Page<Admin> findByAcademyId(Long academyId, Pageable pageable) {
        return jpaAdminRepository.findByAcademyId(academyId, pageable);
    }

    @Override
    public Page<Admin> findAll(Pageable pageable) {
        return jpaAdminRepository.findAll(pageable);
    }

    @Override
    public long countByAcademyId(Long academyId) {
        return jpaAdminRepository.countByAcademyId(academyId);
    }

    @Override
    public boolean existsByUserEmail(String email) {
        return jpaAdminRepository.existsByUserEmail(email);
    }

    @Override
    public void delete(Admin admin) {
        jpaAdminRepository.delete(admin);
    }
}
