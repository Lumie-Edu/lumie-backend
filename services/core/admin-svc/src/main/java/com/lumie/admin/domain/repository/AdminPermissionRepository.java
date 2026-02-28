package com.lumie.admin.domain.repository;

import com.lumie.admin.domain.entity.AdminPermission;

import java.util.List;

public interface AdminPermissionRepository {
    List<AdminPermission> findByAdminId(Long adminId);
    void deleteByAdminId(Long adminId);
    void saveAll(List<AdminPermission> permissions);
}
