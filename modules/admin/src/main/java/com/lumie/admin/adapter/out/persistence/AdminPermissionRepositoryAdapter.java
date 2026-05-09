package com.lumie.admin.adapter.out.persistence;

import com.lumie.admin.domain.entity.AdminPermission;
import com.lumie.admin.domain.repository.AdminPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AdminPermissionRepositoryAdapter implements AdminPermissionRepository {

    private final JpaAdminPermissionRepository jpaRepository;

    @Override
    public List<AdminPermission> findByAdminId(Long adminId) {
        return jpaRepository.findByAdminId(adminId);
    }

    @Override
    public void deleteByAdminId(Long adminId) {
        jpaRepository.deleteByAdminId(adminId);
    }

    @Override
    public void saveAll(List<AdminPermission> permissions) {
        jpaRepository.saveAll(permissions);
    }
}
